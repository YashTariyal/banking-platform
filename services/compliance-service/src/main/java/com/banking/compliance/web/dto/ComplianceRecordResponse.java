package com.banking.compliance.web.dto;

import com.banking.compliance.domain.ComplianceRecordType;
import com.banking.compliance.domain.ComplianceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ComplianceRecordResponse(
        UUID id,
        UUID customerId,
        UUID accountId,
        UUID transactionId,
        ComplianceRecordType recordType,
        ComplianceStatus status,
        BigDecimal amount,
        String currency,
        String description,
        Integer riskScore,
        String flags,
        String sourceEventType,
        String sourceTopic,
        Instant createdAt,
        Instant updatedAt
) {
}

