package com.banking.account.web.dto;

import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        UUID customerId,
        AccountType type,
        AccountStatus status,
        String currency,
        BigDecimal balance,
        Instant openedAt,
        Instant updatedAt
) {
}

