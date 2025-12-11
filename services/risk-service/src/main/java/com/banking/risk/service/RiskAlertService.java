package com.banking.risk.service;

import com.banking.risk.domain.AlertStatus;
import com.banking.risk.domain.RiskAlert;
import com.banking.risk.domain.RiskLevel;
import com.banking.risk.messaging.RiskEventPublisher;
import com.banking.risk.repository.RiskAlertRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RiskAlertService {

    private final RiskAlertRepository alertRepository;
    private final RiskEventPublisher eventPublisher;
    private final Clock clock;

    public RiskAlertService(
            RiskAlertRepository alertRepository,
            RiskEventPublisher eventPublisher,
            Clock clock
    ) {
        this.alertRepository = alertRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public RiskAlert getAlert(UUID alertId) {
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Risk alert not found: " + alertId
                ));
    }

    public Page<RiskAlert> getAlertsByStatus(AlertStatus status, Pageable pageable) {
        return alertRepository.findByStatus(status, pageable);
    }

    public Page<RiskAlert> getAlertsByCustomer(UUID customerId, Pageable pageable) {
        return alertRepository.findByCustomerId(customerId, pageable);
    }

    public Page<RiskAlert> getAlertsByAccount(UUID accountId, Pageable pageable) {
        return alertRepository.findByAccountId(accountId, pageable);
    }

    public Page<RiskAlert> getAlertsByRiskLevel(RiskLevel riskLevel, Pageable pageable) {
        return alertRepository.findByRiskLevel(riskLevel, pageable);
    }

    @Transactional
    public RiskAlert updateAlertStatus(
            UUID alertId,
            AlertStatus status,
            UUID reviewedBy,
            String resolutionNotes
    ) {
        RiskAlert alert = getAlert(alertId);

        if (status == AlertStatus.RESOLVED || status == AlertStatus.FALSE_POSITIVE) {
            alert.setReviewedBy(reviewedBy);
            alert.setReviewedAt(Instant.now(clock));
            alert.setResolutionNotes(resolutionNotes);
        }

        alert.setStatus(status);
        RiskAlert updated = alertRepository.save(alert);

        eventPublisher.publishRiskAlertUpdated(updated);

        return updated;
    }

    public Page<RiskAlert> getOpenAlerts(Pageable pageable) {
        return alertRepository.findByStatus(AlertStatus.OPEN, pageable);
    }
}

