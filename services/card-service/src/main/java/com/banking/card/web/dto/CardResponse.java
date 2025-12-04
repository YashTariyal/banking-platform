package com.banking.card.web.dto;

import com.banking.card.domain.CardStatus;
import com.banking.card.domain.CardType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CardResponse(
        UUID id,
        UUID customerId,
        String maskedNumber,
        CardStatus status,
        CardType type,
        String currency,
        BigDecimal spendingLimit,
        Instant createdAt,
        Instant updatedAt
) {
}


