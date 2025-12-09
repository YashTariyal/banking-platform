package com.banking.card.events;

import com.banking.card.domain.FraudSeverity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class FraudEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FraudEventPublisher.class);
    private static final String FRAUD_EVENTS_TOPIC = "card-fraud-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public FraudEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishFraudDetected(
            UUID cardId,
            UUID customerId,
            FraudSeverity severity,
            BigDecimal fraudScore,
            List<String> riskFactors) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", "FraudDetected");
            payload.put("cardId", cardId.toString());
            payload.put("customerId", customerId != null ? customerId.toString() : "unknown");
            payload.put("severity", severity.name());
            payload.put("fraudScore", fraudScore);
            payload.put("riskFactors", riskFactors);
            payload.put("detectedAt", Instant.now().toString());

            String key = cardId != null ? cardId.toString() : "unknown";
            String serialized = objectMapper.writeValueAsString(payload);

            kafkaTemplate.send(FRAUD_EVENTS_TOPIC, key, serialized)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish fraud event for card {}", cardId, ex);
                        } else {
                            log.debug("Published fraud event for card {}", cardId);
                        }
                    });
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize fraud event for card {}", cardId, ex);
        }
    }
}

