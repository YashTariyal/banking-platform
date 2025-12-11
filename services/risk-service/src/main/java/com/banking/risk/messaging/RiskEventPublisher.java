package com.banking.risk.messaging;

import com.banking.risk.domain.RiskAlert;
import com.banking.risk.domain.RiskAssessment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RiskEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RiskEventPublisher.class);
    private static final String TOPIC = "risk-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RiskEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void publishRiskAssessment(RiskAssessment assessment) {
        publishEvent(createAssessmentEvent("RISK_ASSESSMENT_CREATED", assessment));
    }

    public void publishRiskAlert(RiskAlert alert) {
        publishEvent(createAlertEvent("RISK_ALERT_CREATED", alert));
    }

    public void publishRiskAlertUpdated(RiskAlert alert) {
        publishEvent(createAlertEvent("RISK_ALERT_UPDATED", alert));
    }

    private RiskEvent createAssessmentEvent(String eventType, RiskAssessment assessment) {
        return new RiskEvent(
                eventType,
                assessment.getId(),
                null,
                assessment.getRiskType(),
                assessment.getEntityId(),
                assessment.getCustomerId(),
                assessment.getAccountId(),
                assessment.getRiskLevel(),
                assessment.getRiskScore(),
                assessment.getAmount(),
                assessment.getCurrency(),
                assessment.getRiskFactors(),
                null,
                Instant.now(clock)
        );
    }

    private RiskEvent createAlertEvent(String eventType, RiskAlert alert) {
        return new RiskEvent(
                eventType,
                alert.getRiskAssessmentId(),
                alert.getId(),
                null,
                null,
                alert.getCustomerId(),
                alert.getAccountId(),
                alert.getRiskLevel(),
                alert.getRiskScore(),
                null,
                null,
                null,
                alert.getStatus(),
                Instant.now(clock)
        );
    }

    private void publishEvent(RiskEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = event.assessmentId() != null ? event.assessmentId().toString() :
                    event.alertId() != null ? event.alertId().toString() : "unknown";
            kafkaTemplate.send(TOPIC, key, payload);
            log.debug("Published risk event: {} for assessment/alert: {}", event.eventType(), key);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize risk event: {}", event, e);
        }
    }
}

