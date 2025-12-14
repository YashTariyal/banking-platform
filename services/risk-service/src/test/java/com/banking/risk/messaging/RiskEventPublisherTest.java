package com.banking.risk.messaging;

import com.banking.risk.domain.RiskAlert;
import com.banking.risk.domain.RiskAssessment;
import com.banking.risk.domain.AlertStatus;
import com.banking.risk.domain.RiskLevel;
import com.banking.risk.domain.RiskType;
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
class RiskEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private RiskEventPublisher publisher;
    private RiskAssessment testAssessment;
    private RiskAlert testAlert;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        publisher = new RiskEventPublisher(kafkaTemplate, objectMapper, fixedClock);
        
        testAssessment = new RiskAssessment();
        testAssessment.setId(UUID.randomUUID());
        testAssessment.setRiskType(RiskType.TRANSACTION);
        testAssessment.setEntityId(UUID.randomUUID());
        testAssessment.setCustomerId(UUID.randomUUID());
        testAssessment.setAccountId(UUID.randomUUID());
        testAssessment.setRiskLevel(RiskLevel.HIGH);
        testAssessment.setRiskScore(75);
        testAssessment.setAmount(new BigDecimal("10000.00"));
        testAssessment.setCurrency("USD");
        testAssessment.setRiskFactors("Large amount, Unusual pattern");

        testAlert = new RiskAlert();
        testAlert.setId(UUID.randomUUID());
        testAlert.setRiskAssessmentId(testAssessment.getId());
        testAlert.setCustomerId(UUID.randomUUID());
        testAlert.setAccountId(UUID.randomUUID());
        testAlert.setRiskLevel(RiskLevel.HIGH);
        testAlert.setRiskScore(75);
        testAlert.setStatus(AlertStatus.OPEN);

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.complete(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenReturn(future);
    }

    @Test
    void publishRiskAssessment_PublishesEvent() {
        publisher.publishRiskAssessment(testAssessment);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("risk-events"), any(String.class), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("RISK_ASSESSMENT_CREATED");
        assertThat(payloadCaptor.getValue()).contains(testAssessment.getId().toString());
    }

    @Test
    void publishRiskAlert_PublishesEvent() {
        publisher.publishRiskAlert(testAlert);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("risk-events"), any(String.class), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("RISK_ALERT_CREATED");
        assertThat(payloadCaptor.getValue()).contains(testAlert.getId().toString());
    }

    @Test
    void publishRiskAlertUpdated_PublishesEvent() {
        publisher.publishRiskAlertUpdated(testAlert);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("risk-events"), any(String.class), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("RISK_ALERT_UPDATED");
    }
}
