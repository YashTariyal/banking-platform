package com.banking.risk.web.dto;

import com.banking.risk.domain.AlertStatus;
import com.banking.risk.domain.RiskLevel;
import java.time.Instant;
import java.util.UUID;

public record RiskAlertResponse(
        UUID id,
        UUID riskAssessmentId,
        AlertStatus status,
        RiskLevel riskLevel,
        Integer riskScore,
        UUID customerId,
        UUID accountId,
        String title,
        String description,
        UUID reviewedBy,
        Instant reviewedAt,
        String resolutionNotes,
        Instant createdAt,
        Instant updatedAt
) {
}

