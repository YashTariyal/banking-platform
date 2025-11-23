package com.banking.account.messaging;

import com.banking.account.config.AccountTopicProperties;
import com.banking.account.domain.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.concurrent.CompletableFuture;

@Component
public class AccountEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AccountEventPublisher.class);

    private final KafkaTemplate<String, AccountEvent> kafkaTemplate;
    private final AccountTopicProperties topicProperties;
    private final FailedEventRetryService failedEventRetryService;

    public AccountEventPublisher(
            KafkaTemplate<String, AccountEvent> kafkaTemplate,
            AccountTopicProperties topicProperties,
            FailedEventRetryService failedEventRetryService
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
        this.failedEventRetryService = failedEventRetryService;
    }

    public void publishAccountCreated(Account account) {
        publish(topicProperties.getAccountCreated(), AccountEvent.created(account));
    }

    public void publishAccountUpdated(Account account) {
        publish(topicProperties.getAccountUpdated(), AccountEvent.updated(account));
    }

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2.0)
    )
    private void publish(String topic, AccountEvent event) {
        if (!StringUtils.hasText(topic)) {
            log.warn("Skip publishing account event because topic is not configured. eventType={}", event.eventType());
            return;
        }

        try {
            CompletableFuture<SendResult<String, AccountEvent>> future = kafkaTemplate.send(topic, event.accountId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Published account event successfully. topic={} eventType={} accountId={} offset={}",
                            topic, event.eventType(), event.accountId(), 
                            result.getRecordMetadata() != null ? result.getRecordMetadata().offset() : "unknown");
                } else {
                    log.error("Failed to publish account event. topic={} eventType={} accountId={}", 
                            topic, event.eventType(), event.accountId(), ex);
                    // Send to DLQ for retry
                    failedEventRetryService.scheduleRetry(topic, event);
                }
            });
        } catch (Exception ex) {
            log.error("Exception while publishing account event. topic={} eventType={} accountId={}", 
                    topic, event.eventType(), event.accountId(), ex);
            // Send to DLQ for retry
            failedEventRetryService.scheduleRetry(topic, event);
            throw ex;
        }
    }
}

