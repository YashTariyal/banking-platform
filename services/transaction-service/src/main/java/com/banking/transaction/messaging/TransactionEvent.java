package com.banking.transaction.messaging;

import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionEvent(
        String eventType,
        UUID transactionId,
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
        Instant timestamp
) {
}

