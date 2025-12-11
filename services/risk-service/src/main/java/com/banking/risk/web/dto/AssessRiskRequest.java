package com.banking.risk.web.dto;

import com.banking.risk.domain.RiskType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record AssessRiskRequest(
        @NotNull(message = "Risk type is required")
        RiskType riskType,

        @NotNull(message = "Entity ID is required")
        UUID entityId,

        UUID customerId,

        UUID accountId,

        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        BigDecimal amount,

        @Size(min = 3, max = 3, message = "Currency must be 3 characters")
        String currency,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}

