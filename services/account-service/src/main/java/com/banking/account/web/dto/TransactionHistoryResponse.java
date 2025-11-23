package com.banking.account.web.dto;

import com.banking.account.domain.AccountTransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionHistoryResponse(
        UUID referenceId,
        AccountTransactionType type,
        BigDecimal amount,
        BigDecimal resultingBalance,
        Instant createdAt
) {
}

