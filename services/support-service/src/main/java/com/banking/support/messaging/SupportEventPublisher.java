package com.banking.support.messaging;

import com.banking.support.domain.ManualOverride;
import com.banking.support.domain.SupportCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SupportEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SupportEventPublisher.class);
    private static final String TOPIC = "support-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SupportEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void publishCaseCreated(SupportCase supportCase) {
        publishEvent(createCaseEvent("SUPPORT_CASE_CREATED", supportCase));
    }

    public void publishCaseUpdated(SupportCase supportCase) {
        publishEvent(createCaseEvent("SUPPORT_CASE_UPDATED", supportCase));
    }

    public void publishCaseResolved(SupportCase supportCase) {
        publishEvent(createCaseEvent("SUPPORT_CASE_RESOLVED", supportCase));
    }

    public void publishOverrideCreated(ManualOverride override) {
        publishEvent(createOverrideEvent("MANUAL_OVERRIDE_CREATED", override));
    }

    public void publishOverrideApproved(ManualOverride override) {
        publishEvent(createOverrideEvent("MANUAL_OVERRIDE_APPROVED", override));
    }

    public void publishOverrideRejected(ManualOverride override) {
        publishEvent(createOverrideEvent("MANUAL_OVERRIDE_REJECTED", override));
    }

    public void publishOverrideRevoked(ManualOverride override) {
        publishEvent(createOverrideEvent("MANUAL_OVERRIDE_REVOKED", override));
    }

    private SupportEvent createCaseEvent(String eventType, SupportCase supportCase) {
        return new SupportEvent(
                eventType,
                supportCase.getId(),
                null,
                supportCase.getCaseNumber(),
                supportCase.getCaseType(),
                supportCase.getPriority(),
                supportCase.getStatus(),
                null,
                null,
                supportCase.getCustomerId(),
                supportCase.getAccountId(),
                supportCase.getAssignedTo(),
                supportCase.getCreatedBy(),
                null,
                null,
                null,
                null,
                Instant.now(clock)
        );
    }

    private SupportEvent createOverrideEvent(String eventType, ManualOverride override) {
        return new SupportEvent(
                eventType,
                null,
                override.getId(),
                null,
                null,
                null,
                null,
                override.getOverrideType(),
                override.getStatus(),
                override.getCustomerId(),
                override.getAccountId(),
                null,
                null,
                override.getRequestedBy(),
                override.getApprovedBy(),
                override.getAmount(),
                override.getCurrency(),
                Instant.now(clock)
        );
    }

    private void publishEvent(SupportEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = event.caseId() != null ? event.caseId().toString() :
                    event.overrideId() != null ? event.overrideId().toString() : "unknown";
            kafkaTemplate.send(TOPIC, key, payload);
            log.debug("Published support event: {} for case/override: {}", event.eventType(), key);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize support event: {}", event, e);
        }
    }
}

