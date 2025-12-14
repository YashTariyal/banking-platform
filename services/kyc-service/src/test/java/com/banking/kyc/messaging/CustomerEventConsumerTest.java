package com.banking.kyc.messaging;

import com.banking.kyc.domain.KYCCase;
import com.banking.kyc.domain.KYCStatus;
import com.banking.kyc.domain.RiskLevel;
import com.banking.kyc.repository.KYCCaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerEventConsumerTest {

    @Mock
    private KYCCaseRepository kycCaseRepository;

    private CustomerEventConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new CustomerEventConsumer(kycCaseRepository, objectMapper);
    }

    @Test
    void handleCustomerEvent_WithCustomerCreated_CreatesKYCCase() throws Exception {
        UUID customerId = UUID.randomUUID();
        String eventJson = String.format(
                "{\"eventType\":\"CUSTOMER_CREATED\",\"customerId\":\"%s\"}",
                customerId
        );

        when(kycCaseRepository.findByCustomerIdAndStatusNotIn(eq(customerId), any()))
                .thenReturn(Optional.empty());

        consumer.handleCustomerEvent(eventJson);

        ArgumentCaptor<KYCCase> caseCaptor = ArgumentCaptor.forClass(KYCCase.class);
        verify(kycCaseRepository).save(caseCaptor.capture());

        KYCCase savedCase = caseCaptor.getValue();
        assertThat(savedCase.getCustomerId()).isEqualTo(customerId);
        assertThat(savedCase.getCaseType()).isEqualTo("ONBOARDING");
        assertThat(savedCase.getStatus()).isEqualTo(KYCStatus.PENDING);
        assertThat(savedCase.getRiskLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void handleCustomerEvent_WithExistingKYCCase_DoesNotCreateDuplicate() throws Exception {
        UUID customerId = UUID.randomUUID();
        String eventJson = String.format(
                "{\"eventType\":\"CUSTOMER_CREATED\",\"customerId\":\"%s\"}",
                customerId
        );

        KYCCase existingCase = new KYCCase();
        existingCase.setId(UUID.randomUUID());
        existingCase.setCustomerId(customerId);
        existingCase.setStatus(KYCStatus.PENDING);

        when(kycCaseRepository.findByCustomerIdAndStatusNotIn(eq(customerId), any()))
                .thenReturn(Optional.of(existingCase));

        consumer.handleCustomerEvent(eventJson);

        verify(kycCaseRepository, never()).save(any());
    }

    @Test
    void handleCustomerEvent_WithNonCustomerCreatedEvent_DoesNothing() throws Exception {
        UUID customerId = UUID.randomUUID();
        String eventJson = String.format(
                "{\"eventType\":\"CUSTOMER_UPDATED\",\"customerId\":\"%s\"}",
                customerId
        );

        consumer.handleCustomerEvent(eventJson);

        verify(kycCaseRepository, never()).findByCustomerIdAndStatusNotIn(any(), any());
        verify(kycCaseRepository, never()).save(any());
    }

    @Test
    void handleCustomerEvent_WithInvalidJson_HandlesGracefully() {
        String invalidJson = "{invalid json}";

        consumer.handleCustomerEvent(invalidJson);

        verify(kycCaseRepository, never()).save(any());
    }
}
