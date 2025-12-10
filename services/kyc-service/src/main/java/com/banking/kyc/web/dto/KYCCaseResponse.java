package com.banking.kyc.web.dto;

import com.banking.kyc.domain.KYCStatus;
import com.banking.kyc.domain.RiskLevel;
import java.time.Instant;
import java.util.UUID;

public record KYCCaseResponse(
        UUID id,
        UUID customerId,
        KYCStatus status,
        RiskLevel riskLevel,
        String caseType,
        UUID assignedTo,
        String reviewNotes,
        Boolean screeningCompleted,
        Boolean documentVerificationCompleted,
        Instant approvedAt,
        Instant rejectedAt,
        String rejectionReason,
        Instant dueDate,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

