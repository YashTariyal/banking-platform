package com.banking.account.web.dto;

import com.banking.account.domain.AccountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BulkCreateAccountRequest(
        @NotNull UUID customerId,
        @NotEmpty
        @Size(min = 1, max = 100, message = "Must create between 1 and 100 accounts at a time")
        @Valid List<AccountCreationItem> accounts
) {
    public record AccountCreationItem(
            @NotNull AccountType type,
            @NotNull String currency,
            @NotNull BigDecimal initialDeposit
    ) {
    }
}

