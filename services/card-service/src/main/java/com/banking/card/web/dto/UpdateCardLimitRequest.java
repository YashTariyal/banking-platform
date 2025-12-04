package com.banking.card.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdateCardLimitRequest(
        @NotNull @Positive BigDecimal spendingLimit
) {
}


