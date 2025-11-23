package com.banking.account.web.dto;

import com.banking.account.domain.AccountTransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BulkTransactionRequest(
        @NotEmpty
        @Size(min = 1, max = 100, message = "Must process between 1 and 100 transactions at a time")
        @Valid List<TransactionItem> transactions
) {
    public record TransactionItem(
            @NotNull UUID accountId,
            @NotNull UUID referenceId,
            @NotNull AccountTransactionType type,
            @NotNull BigDecimal amount,
            String description
    ) {
    }
}

