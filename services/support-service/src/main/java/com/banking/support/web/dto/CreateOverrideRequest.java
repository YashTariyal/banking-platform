package com.banking.support.web.dto;

import com.banking.support.domain.OverrideType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateOverrideRequest(
        @NotNull(message = "Override type is required")
        OverrideType overrideType,

        UUID customerId,

        UUID accountId,

        UUID entityId,

        @NotBlank(message = "Reason is required")
        @Size(max = 5000, message = "Reason must not exceed 5000 characters")
        String reason,

        @Size(max = 1000, message = "Override value must not exceed 1000 characters")
        String overrideValue,

        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        BigDecimal amount,

        @Size(min = 3, max = 3, message = "Currency must be 3 characters")
        String currency,

        Instant expiresAt
) {
}

