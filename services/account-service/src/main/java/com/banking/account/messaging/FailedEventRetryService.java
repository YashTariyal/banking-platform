package com.banking.account.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for retrying failed Kafka events.
 * Implements exponential backoff retry mechanism and DLQ support.
 */
@Service
public class FailedEventRetryService {

    private static final Logger log = LoggerFactory.getLogger(FailedEventRetryService.class);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_SECONDS = 5;
    private static final String DLQ_TOPIC_SUFFIX = ".dlq";

    private final KafkaTemplate<String, AccountEvent> kafkaTemplate;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final ConcurrentHashMap<String, RetryContext> retryContexts = new ConcurrentHashMap<>();

    public FailedEventRetryService(KafkaTemplate<String, AccountEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Async
    public void scheduleRetry(String topic, AccountEvent event) {
        String retryKey = event.accountId().toString() + "-" + event.eventType();
        RetryContext context = retryContexts.computeIfAbsent(retryKey, k -> new RetryContext(topic, event));
        
        if (context.retryCount >= MAX_RETRIES) {
            log.error("Max retries exceeded for event. Sending to DLQ. topic={} eventType={} accountId={} retries={}",
                    topic, event.eventType(), event.accountId(), context.retryCount);
            sendToDLQ(topic, event);
            retryContexts.remove(retryKey);
            return;
        }

        context.retryCount++;
        long delay = (long) (INITIAL_DELAY_SECONDS * Math.pow(2, context.retryCount - 1));
        
        log.info("Scheduling retry for failed event. topic={} eventType={} accountId={} retry={}/{} delay={}s",
                topic, event.eventType(), event.accountId(), context.retryCount, MAX_RETRIES, delay);

        scheduler.schedule(() -> {
            try {
                kafkaTemplate.send(topic, event.accountId().toString(), event).get(5, TimeUnit.SECONDS);
                log.info("Successfully retried event. topic={} eventType={} accountId={} retry={}",
                        topic, event.eventType(), event.accountId(), context.retryCount);
                retryContexts.remove(retryKey);
            } catch (Exception ex) {
                log.warn("Retry failed. topic={} eventType={} accountId={} retry={}",
                        topic, event.eventType(), event.accountId(), context.retryCount, ex);
                // Schedule next retry
                scheduleRetry(topic, event);
            }
        }, delay, TimeUnit.SECONDS);
    }

    private void sendToDLQ(String originalTopic, AccountEvent event) {
        String dlqTopic = originalTopic + DLQ_TOPIC_SUFFIX;
        try {
            kafkaTemplate.send(dlqTopic, event.accountId().toString(), event);
            log.info("Sent failed event to DLQ. dlqTopic={} eventType={} accountId={}",
                    dlqTopic, event.eventType(), event.accountId());
        } catch (Exception ex) {
            log.error("Failed to send event to DLQ. dlqTopic={} eventType={} accountId={}",
                    dlqTopic, event.eventType(), event.accountId(), ex);
        }
    }

    private static class RetryContext {
        final String topic;
        final AccountEvent event;
        final Instant createdAt;
        int retryCount;

        RetryContext(String topic, AccountEvent event) {
            this.topic = topic;
            this.event = event;
            this.createdAt = Instant.now();
            this.retryCount = 0;
        }
    }
}

