package com.banking.kyc.messaging;

import com.banking.kyc.domain.KYCCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KYCEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KYCEventPublisher.class);
    private static final String KYC_EVENTS_TOPIC = "kyc-events";

    private final KafkaTemplate<String, KYCEvent> kafkaTemplate;

    public KYCEventPublisher(KafkaTemplate<String, KYCEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishKYCCaseCreated(KYCCase kycCase) {
        publishEvent(KYCEvent.created(kycCase), kycCase.getId().toString());
    }

    public void publishKYCCaseUpdated(KYCCase kycCase) {
        publishEvent(KYCEvent.updated(kycCase), kycCase.getId().toString());
    }

    public void publishKYCCaseApproved(KYCCase kycCase) {
        publishEvent(KYCEvent.approved(kycCase), kycCase.getId().toString());
    }

    public void publishKYCCaseRejected(KYCCase kycCase) {
        publishEvent(KYCEvent.rejected(kycCase), kycCase.getId().toString());
    }

    private void publishEvent(KYCEvent event, String key) {
        try {
            kafkaTemplate.send(KYC_EVENTS_TOPIC, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} event for KYC case {}", event.eventType(), event.kycCaseId(), ex);
                        } else {
                            log.debug("Published {} event for KYC case {}", event.eventType(), event.kycCaseId());
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing {} event for KYC case {}", event.eventType(), event.kycCaseId(), e);
        }
    }
}

