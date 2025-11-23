package com.banking.account.web.dto;

import com.banking.account.domain.AccountTransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountTransactionRequest(
        @NotNull UUID referenceId,
        @NotNull AccountTransactionType type,
        @NotNull @Positive BigDecimal amount,
        String description
) {
}

