package com.banking.card.web.dto;

import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdateAtmLimitsRequest(
        @Positive(message = "Daily ATM limit must be positive")
        BigDecimal dailyLimit,
        @Positive(message = "Monthly ATM limit must be positive")
        BigDecimal monthlyLimit
) {
}

