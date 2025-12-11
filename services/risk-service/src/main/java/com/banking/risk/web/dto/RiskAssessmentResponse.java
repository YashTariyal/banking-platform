package com.banking.risk.web.dto;

import com.banking.risk.domain.RiskLevel;
import com.banking.risk.domain.RiskType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RiskAssessmentResponse(
        UUID id,
        RiskType riskType,
        UUID entityId,
        RiskLevel riskLevel,
        Integer riskScore,
        UUID customerId,
        UUID accountId,
        BigDecimal amount,
        String currency,
        String riskFactors,
        String description,
        Instant assessedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

