package com.banking.card.web.dto;

import com.banking.card.domain.AuthorizationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AuthorizationRequestDto(
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,
        @Size(min = 3, max = 3, message = "Currency must be 3 characters")
        String currency,
        String merchantName,
        @Size(max = 10) String merchantCategoryCode,
        @Size(min = 2, max = 2) String merchantCountry
) {
}

