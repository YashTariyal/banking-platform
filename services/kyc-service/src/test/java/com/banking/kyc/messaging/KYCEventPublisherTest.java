package com.banking.kyc.messaging;

import com.banking.kyc.domain.KYCCase;
import com.banking.kyc.domain.KYCStatus;
import com.banking.kyc.domain.RiskLevel;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KYCEventPublisherTest {

    @Mock
    private KafkaTemplate<String, KYCEvent> kafkaTemplate;

    private KYCEventPublisher publisher;
    private KYCCase testCase;

    @BeforeEach
    void setUp() {
        publisher = new KYCEventPublisher(kafkaTemplate);
        
        testCase = new KYCCase();
        testCase.setId(UUID.randomUUID());
        testCase.setCustomerId(UUID.randomUUID());
        testCase.setCaseType("ONBOARDING");
        testCase.setStatus(KYCStatus.PENDING);
        testCase.setRiskLevel(RiskLevel.LOW);

        CompletableFuture<SendResult<String, KYCEvent>> future = new CompletableFuture<>();
        future.complete(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(KYCEvent.class)))
                .thenReturn(future);
    }

    @Test
    void publishKYCCaseCreated_PublishesEvent() {
        publisher.publishKYCCaseCreated(testCase);

        ArgumentCaptor<KYCEvent> eventCaptor = ArgumentCaptor.forClass(KYCEvent.class);
        verify(kafkaTemplate).send(eq("kyc-events"), eq(testCase.getId().toString()), eventCaptor.capture());

        assertThat(eventCaptor.getValue().eventType()).isEqualTo("KYC_CASE_CREATED");
        assertThat(eventCaptor.getValue().kycCaseId()).isEqualTo(testCase.getId());
    }

    @Test
    void publishKYCCaseUpdated_PublishesEvent() {
        publisher.publishKYCCaseUpdated(testCase);

        ArgumentCaptor<KYCEvent> eventCaptor = ArgumentCaptor.forClass(KYCEvent.class);
        verify(kafkaTemplate).send(eq("kyc-events"), eq(testCase.getId().toString()), eventCaptor.capture());

        assertThat(eventCaptor.getValue().eventType()).isEqualTo("KYC_CASE_UPDATED");
    }

    @Test
    void publishKYCCaseApproved_PublishesEvent() {
        publisher.publishKYCCaseApproved(testCase);

        ArgumentCaptor<KYCEvent> eventCaptor = ArgumentCaptor.forClass(KYCEvent.class);
        verify(kafkaTemplate).send(eq("kyc-events"), eq(testCase.getId().toString()), eventCaptor.capture());

        assertThat(eventCaptor.getValue().eventType()).isEqualTo("KYC_CASE_APPROVED");
    }

    @Test
    void publishKYCCaseRejected_PublishesEvent() {
        publisher.publishKYCCaseRejected(testCase);

        ArgumentCaptor<KYCEvent> eventCaptor = ArgumentCaptor.forClass(KYCEvent.class);
        verify(kafkaTemplate).send(eq("kyc-events"), eq(testCase.getId().toString()), eventCaptor.capture());

        assertThat(eventCaptor.getValue().eventType()).isEqualTo("KYC_CASE_REJECTED");
    }
}
