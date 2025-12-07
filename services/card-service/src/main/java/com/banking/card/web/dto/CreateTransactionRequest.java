package com.banking.card.web.dto;

import com.banking.card.domain.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateTransactionRequest(
        @NotNull TransactionType transactionType,
        @NotNull @Positive BigDecimal amount,
        @Size(min = 3, max = 3) String currency,
        String merchantName,
        @Size(max = 10) String merchantCategoryCode,
        @Size(min = 2, max = 2) String merchantCountry
) {
}

