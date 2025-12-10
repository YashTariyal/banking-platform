package com.banking.kyc.web.dto;

import com.banking.kyc.domain.KYCStatus;
import com.banking.kyc.domain.RiskLevel;
import java.time.Instant;
import java.util.UUID;

public record UpdateKYCCaseRequest(
        KYCStatus status,
        RiskLevel riskLevel,
        UUID assignedTo,
        String reviewNotes,
        Instant dueDate
) {
}

