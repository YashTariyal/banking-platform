package com.banking.support.messaging;

import com.banking.support.domain.ManualOverride;
import com.banking.support.domain.OverrideStatus;
import com.banking.support.domain.OverrideType;
import com.banking.support.domain.SupportCase;
import com.banking.support.domain.CasePriority;
import com.banking.support.domain.CaseStatus;
import com.banking.support.domain.CaseType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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
class SupportEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private SupportEventPublisher publisher;
    private SupportCase testCase;
    private ManualOverride testOverride;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        publisher = new SupportEventPublisher(kafkaTemplate, objectMapper, fixedClock);
        
        testCase = new SupportCase();
        testCase.setId(UUID.randomUUID());
        testCase.setCaseNumber("CASE1234567890");
        testCase.setCaseType(CaseType.ACCOUNT_INQUIRY);
        testCase.setPriority(CasePriority.HIGH);
        testCase.setStatus(CaseStatus.OPEN);
        testCase.setCustomerId(UUID.randomUUID());
        testCase.setAccountId(UUID.randomUUID());
        testCase.setCreatedBy(UUID.randomUUID());

        testOverride = new ManualOverride();
        testOverride.setId(UUID.randomUUID());
        testOverride.setOverrideType(OverrideType.ACCOUNT_LIMIT);
        testOverride.setStatus(OverrideStatus.PENDING);
        testOverride.setCustomerId(UUID.randomUUID());
        testOverride.setAccountId(UUID.randomUUID());
        testOverride.setRequestedBy(UUID.randomUUID());
        testOverride.setAmount(new BigDecimal("10000.00"));
        testOverride.setCurrency("USD");

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.complete(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenReturn(future);
    }

    @Test
    void publishCaseCreated_PublishesEvent() {
        publisher.publishCaseCreated(testCase);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("support-events"), eq(testCase.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("SUPPORT_CASE_CREATED");
        assertThat(payloadCaptor.getValue()).contains(testCase.getId().toString());
    }

    @Test
    void publishCaseUpdated_PublishesEvent() {
        publisher.publishCaseUpdated(testCase);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("support-events"), eq(testCase.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("SUPPORT_CASE_UPDATED");
    }

    @Test
    void publishCaseResolved_PublishesEvent() {
        publisher.publishCaseResolved(testCase);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("support-events"), eq(testCase.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("SUPPORT_CASE_RESOLVED");
    }

    @Test
    void publishOverrideCreated_PublishesEvent() {
        publisher.publishOverrideCreated(testOverride);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("support-events"), eq(testOverride.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("MANUAL_OVERRIDE_CREATED");
    }

    @Test
    void publishOverrideApproved_PublishesEvent() {
        publisher.publishOverrideApproved(testOverride);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("support-events"), eq(testOverride.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("MANUAL_OVERRIDE_APPROVED");
    }

    @Test
    void publishOverrideRejected_PublishesEvent() {
        publisher.publishOverrideRejected(testOverride);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("support-events"), eq(testOverride.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("MANUAL_OVERRIDE_REJECTED");
    }

    @Test
    void publishOverrideRevoked_PublishesEvent() {
        publisher.publishOverrideRevoked(testOverride);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("support-events"), eq(testOverride.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("MANUAL_OVERRIDE_REVOKED");
    }
}
