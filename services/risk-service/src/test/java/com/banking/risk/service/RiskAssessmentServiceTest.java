package com.banking.risk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.risk.domain.AlertStatus;
import com.banking.risk.domain.RiskAlert;
import com.banking.risk.domain.RiskAssessment;
import com.banking.risk.domain.RiskLevel;
import com.banking.risk.domain.RiskType;
import com.banking.risk.messaging.RiskEventPublisher;
import com.banking.risk.repository.RiskAlertRepository;
import com.banking.risk.repository.RiskAssessmentRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceTest {

    @Mock
    private RiskAssessmentRepository assessmentRepository;

    @Mock
    private RiskAlertRepository alertRepository;

    @Mock
    private RiskScoringService scoringService;

    @Mock
    private RiskEventPublisher eventPublisher;

    private Clock fixedClock;
    private RiskAssessmentService assessmentService;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));
        assessmentService = new RiskAssessmentService(
                assessmentRepository,
                alertRepository,
                scoringService,
                eventPublisher,
                fixedClock
        );
    }

    @Test
    void assessRisk_lowRisk_createsAssessmentWithoutAlert() {
        UUID entityId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100");

        RiskScoringService.RiskScoreResult scoreResult = new RiskScoringService.RiskScoreResult(
                20, RiskLevel.LOW, List.of("Low risk transaction")
        );

        when(scoringService.calculateRiskScore(any(), any(), any(), any(), any()))
                .thenReturn(scoreResult);
        when(assessmentRepository.save(any(RiskAssessment.class)))
                .thenAnswer(invocation -> {
                    RiskAssessment assessment = invocation.getArgument(0);
                    assessment.setId(UUID.randomUUID());
                    return assessment;
                });

        RiskAssessment result = assessmentService.assessRisk(
                RiskType.TRANSACTION,
                entityId,
                customerId,
                accountId,
                amount,
                "USD",
                "Test transaction",
                null
        );

        assertThat(result).isNotNull();
        assertThat(result.getRiskType()).isEqualTo(RiskType.TRANSACTION);
        assertThat(result.getEntityId()).isEqualTo(entityId);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.getRiskScore()).isEqualTo(20);

        verify(assessmentRepository).save(any(RiskAssessment.class));
        verify(alertRepository, never()).save(any(RiskAlert.class));
        verify(eventPublisher).publishRiskAssessment(any(RiskAssessment.class));
    }

    @Test
    void assessRisk_highRisk_createsAssessmentAndAlert() {
        UUID entityId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("15000");

        RiskScoringService.RiskScoreResult scoreResult = new RiskScoringService.RiskScoreResult(
                85, RiskLevel.CRITICAL, List.of("Large transaction amount", "High velocity")
        );

        when(scoringService.calculateRiskScore(any(), any(), any(), any(), any()))
                .thenReturn(scoreResult);
        when(assessmentRepository.save(any(RiskAssessment.class)))
                .thenAnswer(invocation -> {
                    RiskAssessment assessment = invocation.getArgument(0);
                    assessment.setId(UUID.randomUUID());
                    return assessment;
                });
        when(alertRepository.save(any(RiskAlert.class)))
                .thenAnswer(invocation -> {
                    RiskAlert alert = invocation.getArgument(0);
                    alert.setId(UUID.randomUUID());
                    return alert;
                });

        RiskAssessment result = assessmentService.assessRisk(
                RiskType.TRANSACTION,
                entityId,
                customerId,
                accountId,
                amount,
                "USD",
                "High risk transaction",
                null
        );

        assertThat(result).isNotNull();
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);

        ArgumentCaptor<RiskAlert> alertCaptor = ArgumentCaptor.forClass(RiskAlert.class);
        verify(alertRepository).save(alertCaptor.capture());
        RiskAlert alert = alertCaptor.getValue();
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(alert.getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(alert.getRiskAssessmentId()).isEqualTo(result.getId());

        verify(eventPublisher).publishRiskAlert(any(RiskAlert.class));
    }

    @Test
    void getAssessment_exists_returnsAssessment() {
        UUID assessmentId = UUID.randomUUID();
        RiskAssessment assessment = new RiskAssessment();
        assessment.setId(assessmentId);
        assessment.setRiskType(RiskType.TRANSACTION);

        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment));

        RiskAssessment result = assessmentService.getAssessment(assessmentId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(assessmentId);
    }

    @Test
    void getAssessment_notExists_throwsException() {
        UUID assessmentId = UUID.randomUUID();

        when(assessmentRepository.findById(assessmentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> assessmentService.getAssessment(assessmentId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void getAssessmentsByType_returnsPage() {
        PageRequest pageable = PageRequest.of(0, 20);
        RiskAssessment assessment = new RiskAssessment();
        assessment.setId(UUID.randomUUID());
        assessment.setRiskType(RiskType.TRANSACTION);

        when(assessmentRepository.findByRiskType(RiskType.TRANSACTION, pageable))
                .thenReturn(new PageImpl<>(List.of(assessment), pageable, 1));

        Page<RiskAssessment> result = assessmentService.getAssessmentsByType(RiskType.TRANSACTION, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getAssessmentsByCustomer_returnsPage() {
        UUID customerId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        RiskAssessment assessment = new RiskAssessment();
        assessment.setId(UUID.randomUUID());
        assessment.setCustomerId(customerId);

        when(assessmentRepository.findByCustomerId(customerId, pageable))
                .thenReturn(new PageImpl<>(List.of(assessment), pageable, 1));

        Page<RiskAssessment> result = assessmentService.getAssessmentsByCustomer(customerId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }
}

