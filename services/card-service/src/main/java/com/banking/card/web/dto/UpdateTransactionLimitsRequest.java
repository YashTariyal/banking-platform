package com.banking.card.web.dto;

import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdateTransactionLimitsRequest(
        @Positive(message = "Daily transaction limit must be positive")
        BigDecimal dailyLimit,
        @Positive(message = "Monthly transaction limit must be positive")
        BigDecimal monthlyLimit
) {
}

