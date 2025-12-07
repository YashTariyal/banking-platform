package com.banking.card.web.dto;

import com.banking.card.domain.CardStatus;
import com.banking.card.domain.CardType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
        Instant updatedAt,
        String cancellationReason,
        // Transaction limits
        BigDecimal dailyTransactionLimit,
        BigDecimal monthlyTransactionLimit,
        // PIN management (never expose PIN hash, only status info)
        Boolean pinSet,
        Boolean pinLocked,
        Instant pinLockedUntil,
        // Freeze/unfreeze
        Boolean frozen,
        Instant frozenAt,
        String frozenReason,
        // Expiration
        LocalDate expirationDate,
        Instant issuedAt,
        // Replacement
        UUID replacedByCardId,
        String replacementReason,
        Boolean isReplacement,
        // Account linking
        UUID accountId,
        // CVV management (never expose CVV, only status info)
        Boolean cvvSet,
        Instant cvvGeneratedAt,
        Instant cvvRotationDueDate,
        // Cardholder name
        String cardholderName,
        // ATM withdrawal limits
        BigDecimal dailyAtmLimit,
        BigDecimal monthlyAtmLimit,
        // Renewal tracking
        UUID renewedFromCardId,
        Integer renewalCount,
        Instant lastRenewedAt,
        // Contactless payment
        Boolean contactlessEnabled
) {
}


