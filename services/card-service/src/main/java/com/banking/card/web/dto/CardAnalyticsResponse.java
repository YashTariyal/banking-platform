package com.banking.card.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CardAnalyticsResponse(
        UUID cardId,
        Integer totalTransactions,
        BigDecimal totalAmount,
        BigDecimal averageTransactionAmount,
        Integer declinedTransactions,
        Instant lastTransactionDate,
        String topMerchantCategory,
        String mostUsedCountry,
        Instant lastUpdatedAt
) {
}

