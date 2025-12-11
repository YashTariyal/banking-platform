package com.banking.risk.messaging;

import com.banking.risk.domain.AlertStatus;
import com.banking.risk.domain.RiskLevel;
import com.banking.risk.domain.RiskType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RiskEvent(
        String eventType,
        UUID assessmentId,
        UUID alertId,
        RiskType riskType,
        UUID entityId,
        UUID customerId,
        UUID accountId,
        RiskLevel riskLevel,
        Integer riskScore,
        BigDecimal amount,
        String currency,
        String riskFactors,
        AlertStatus alertStatus,
        Instant timestamp
) {
}

