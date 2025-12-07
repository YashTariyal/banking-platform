package com.banking.card.web.dto;

import com.banking.card.domain.TransactionStatus;
import com.banking.card.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CardTransactionResponse(
        UUID id,
        UUID cardId,
        TransactionType transactionType,
        BigDecimal amount,
        String currency,
        String merchantName,
        String merchantCategoryCode,
        String merchantCountry,
        Instant transactionDate,
        TransactionStatus status,
        String declineReason,
        Instant createdAt
) {
}

