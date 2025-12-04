package com.banking.card.web.dto;

import com.banking.card.domain.CardType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateCardRequest(
        @NotNull UUID customerId,
        @NotNull CardType type,
        @Size(min = 3, max = 3) String currency,
        @NotNull @Positive BigDecimal spendingLimit
) {
}


