package com.banking.compliance.messaging;

import com.banking.compliance.service.AMLService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final AMLService amlService;
    private final ObjectMapper objectMapper;

    public TransactionEventConsumer(AMLService amlService, ObjectMapper objectMapper) {
        this.amlService = amlService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"transaction-events", "payment-events", "card-events"}, groupId = "compliance-service")
    public void consumeTransactionEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Received event from topic {} with key {}", topic, key);
            JsonNode event = objectMapper.readTree(message);

            String eventType = event.has("eventType") ? event.get("eventType").asText() : "UNKNOWN";
            UUID customerId = extractUUID(event, "customerId");
            UUID accountId = extractUUID(event, "accountId");
            UUID transactionId = extractUUID(event, "transactionId") != null
                    ? extractUUID(event, "transactionId")
                    : extractUUID(event, "id");

            BigDecimal amount = null;
            if (event.has("amount")) {
                amount = new BigDecimal(event.get("amount").asText());
            }

            String currency = event.has("currency") ? event.get("currency").asText() : null;

            if (customerId != null && amount != null) {
                amlService.analyzeTransaction(
                        customerId,
                        accountId,
                        transactionId,
                        amount,
                        currency,
                        eventType,
                        topic
                );
                log.debug("Processed compliance check for transaction {}", transactionId);
            } else {
                log.warn("Skipping event - missing required fields: customerId={}, amount={}", customerId, amount);
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing transaction event from topic {}: {}", topic, e.getMessage(), e);
            // In production, you might want to send to DLQ or retry
            acknowledgment.acknowledge(); // Acknowledge to prevent infinite retries
        }
    }

    private UUID extractUUID(JsonNode node, String fieldName) {
        if (!node.has(fieldName)) {
            return null;
        }
        try {
            String value = node.get(fieldName).asText();
            return value != null && !value.isEmpty() ? UUID.fromString(value) : null;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format for field {}: {}", fieldName, node.get(fieldName).asText());
            return null;
        }
    }
}

