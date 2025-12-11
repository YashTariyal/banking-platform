package com.banking.risk.service;

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
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RiskAssessmentService {

    private final RiskAssessmentRepository assessmentRepository;
    private final RiskAlertRepository alertRepository;
    private final RiskScoringService scoringService;
    private final RiskEventPublisher eventPublisher;
    private final Clock clock;

    public RiskAssessmentService(
            RiskAssessmentRepository assessmentRepository,
            RiskAlertRepository alertRepository,
            RiskScoringService scoringService,
            RiskEventPublisher eventPublisher,
            Clock clock
    ) {
        this.assessmentRepository = assessmentRepository;
        this.alertRepository = alertRepository;
        this.scoringService = scoringService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public RiskAssessment assessRisk(
            RiskType riskType,
            UUID entityId,
            UUID customerId,
            UUID accountId,
            BigDecimal amount,
            String currency,
            String description,
            RiskScoringService.RiskScoringContext context
    ) {
        RiskScoringService.RiskScoreResult scoreResult = scoringService.calculateRiskScore(
                amount, currency, customerId, accountId, context
        );

        RiskAssessment assessment = new RiskAssessment();
        assessment.setRiskType(riskType);
        assessment.setEntityId(entityId);
        assessment.setCustomerId(customerId);
        assessment.setAccountId(accountId);
        assessment.setAmount(amount);
        assessment.setCurrency(currency);
        assessment.setDescription(description);
        assessment.setRiskScore(scoreResult.score());
        assessment.setRiskLevel(scoreResult.level());
        assessment.setRiskFactors(String.join(", ", scoreResult.riskFactors()));
        assessment.setAssessedAt(Instant.now(clock));

        RiskAssessment saved = assessmentRepository.save(assessment);

        // Create alert if risk level is MEDIUM or higher
        if (scoreResult.level().ordinal() >= RiskLevel.MEDIUM.ordinal()) {
            createRiskAlert(saved);
        }

        // Publish risk assessment event
        eventPublisher.publishRiskAssessment(saved);

        return saved;
    }

    @Transactional
    private void createRiskAlert(RiskAssessment assessment) {
        RiskAlert alert = new RiskAlert();
        alert.setRiskAssessmentId(assessment.getId());
        alert.setStatus(AlertStatus.OPEN);
        alert.setRiskLevel(assessment.getRiskLevel());
        alert.setRiskScore(assessment.getRiskScore());
        alert.setCustomerId(assessment.getCustomerId());
        alert.setAccountId(assessment.getAccountId());
        alert.setTitle("Risk Alert: " + assessment.getRiskType() + " - " + assessment.getRiskLevel());
        alert.setDescription("Risk assessment detected: " + assessment.getRiskFactors());

        RiskAlert saved = alertRepository.save(alert);
        eventPublisher.publishRiskAlert(saved);
    }

    public RiskAssessment getAssessment(UUID assessmentId) {
        return assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Risk assessment not found: " + assessmentId
                ));
    }

    public Page<RiskAssessment> getAssessmentsByType(RiskType riskType, Pageable pageable) {
        return assessmentRepository.findByRiskType(riskType, pageable);
    }

    public Page<RiskAssessment> getAssessmentsByCustomer(UUID customerId, Pageable pageable) {
        return assessmentRepository.findByCustomerId(customerId, pageable);
    }

    public Page<RiskAssessment> getAssessmentsByAccount(UUID accountId, Pageable pageable) {
        return assessmentRepository.findByAccountId(accountId, pageable);
    }

    public Page<RiskAssessment> getAssessmentsByRiskLevel(RiskLevel riskLevel, Pageable pageable) {
        return assessmentRepository.findByRiskLevel(riskLevel, pageable);
    }

    public List<RiskAssessment> getAssessmentsByEntity(RiskType riskType, UUID entityId) {
        return assessmentRepository.findByEntityIdAndRiskType(entityId, riskType);
    }
}

