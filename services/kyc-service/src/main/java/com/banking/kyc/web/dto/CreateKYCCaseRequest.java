package com.banking.kyc.web.dto;

import com.banking.kyc.domain.RiskLevel;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateKYCCaseRequest(
        @NotNull UUID customerId,
        String caseType,
        RiskLevel riskLevel,
        UUID assignedTo,
        Instant dueDate
) {
}

