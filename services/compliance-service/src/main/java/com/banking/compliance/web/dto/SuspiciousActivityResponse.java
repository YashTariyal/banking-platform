package com.banking.compliance.web.dto;

import com.banking.compliance.domain.Severity;
import com.banking.compliance.domain.SuspiciousActivityStatus;
import com.banking.compliance.domain.SuspiciousActivityType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SuspiciousActivityResponse(
        UUID id,
        UUID customerId,
        UUID accountId,
        UUID transactionId,
        SuspiciousActivityType activityType,
        Severity severity,
        SuspiciousActivityStatus status,
        BigDecimal amount,
        String currency,
        String description,
        Integer riskScore,
        UUID complianceRecordId,
        UUID investigatorId,
        String investigationNotes,
        Instant reportedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

