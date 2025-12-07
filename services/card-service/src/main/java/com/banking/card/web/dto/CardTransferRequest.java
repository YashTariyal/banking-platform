package com.banking.card.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CardTransferRequest(
        @NotNull(message = "Destination card ID is required")
        UUID toCardId,
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,
        @Size(min = 3, max = 3, message = "Currency must be 3 characters")
        String currency
) {
}

