package com.banking.transaction.messaging;

import com.banking.transaction.domain.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisher.class);
    private static final String TOPIC = "transaction-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public TransactionEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void publishTransactionInitiated(Transaction transaction) {
        publishEvent(createEvent("TRANSACTION_INITIATED", transaction));
    }

    public void publishTransactionProcessing(Transaction transaction) {
        publishEvent(createEvent("TRANSACTION_PROCESSING", transaction));
    }

    public void publishTransactionCompleted(Transaction transaction) {
        publishEvent(createEvent("TRANSACTION_COMPLETED", transaction));
    }

    public void publishTransactionFailed(Transaction transaction) {
        publishEvent(createEvent("TRANSACTION_FAILED", transaction));
    }

    public void publishTransactionCancelled(Transaction transaction) {
        publishEvent(createEvent("TRANSACTION_CANCELLED", transaction));
    }

    public void publishTransactionReversed(Transaction transaction) {
        publishEvent(createEvent("TRANSACTION_REVERSED", transaction));
    }

    private TransactionEvent createEvent(String eventType, Transaction transaction) {
        return new TransactionEvent(
                eventType,
                transaction.getId(),
                transaction.getReferenceId(),
                transaction.getTransactionType(),
                transaction.getStatus(),
                transaction.getCustomerId(),
                transaction.getFromAccountId(),
                transaction.getToAccountId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getDescription(),
                transaction.getFailureReason(),
                Instant.now(clock)
        );
    }

    private void publishEvent(TransactionEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = event.transactionId().toString();
            kafkaTemplate.send(TOPIC, key, payload);
            log.debug("Published transaction event: {} for transaction: {}", event.eventType(), key);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize transaction event: {}", event, e);
        }
    }
}

