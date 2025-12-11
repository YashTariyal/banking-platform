package com.banking.transaction.web.dto;

import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String referenceId,
        TransactionType transactionType,
        TransactionStatus status,
        UUID customerId,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        String description,
        String failureReason,
        Instant initiatedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

