package com.banking.kyc.messaging;

import com.banking.kyc.domain.KYCCase;
import com.banking.kyc.domain.KYCStatus;
import com.banking.kyc.domain.RiskLevel;
import com.banking.kyc.repository.KYCCaseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CustomerEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CustomerEventConsumer.class);

    private final KYCCaseRepository kycCaseRepository;
    private final ObjectMapper objectMapper;

    public CustomerEventConsumer(KYCCaseRepository kycCaseRepository, ObjectMapper objectMapper) {
        this.kycCaseRepository = kycCaseRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "customer-events", groupId = "kyc-service")
    public void handleCustomerEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = event.path("eventType").asText();
            UUID customerId = UUID.fromString(event.path("customerId").asText());

            if ("CUSTOMER_CREATED".equals(eventType)) {
                createOnboardingKYCCase(customerId);
            }
        } catch (Exception e) {
            log.error("Error processing customer event: {}", message, e);
        }
    }

    private void createOnboardingKYCCase(UUID customerId) {
        try {
            // Check if KYC case already exists
            List<KYCStatus> excludedStatuses = List.of(KYCStatus.APPROVED, KYCStatus.REJECTED);
            kycCaseRepository.findByCustomerIdAndStatusNotIn(customerId, excludedStatuses)
                    .ifPresentOrElse(
                            existing -> log.debug("KYC case already exists for customer: {}", customerId),
                            () -> {
                                KYCCase kycCase = new KYCCase();
                                kycCase.setCustomerId(customerId);
                                kycCase.setCaseType("ONBOARDING");
                                kycCase.setStatus(KYCStatus.PENDING);
                                kycCase.setRiskLevel(RiskLevel.LOW);
                                kycCaseRepository.save(kycCase);
                                log.info("Created onboarding KYC case for customer: {}", customerId);
                            }
                    );
        } catch (Exception e) {
            log.error("Error creating onboarding KYC case for customer: {}", customerId, e);
        }
    }
}

