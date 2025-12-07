package com.banking.card.web;

import com.banking.card.domain.Card;
import com.banking.card.web.dto.CardResponse;
import java.time.Instant;

public final class CardMapper {

    private CardMapper() {
    }

    public static CardResponse toResponse(Card card) {
        // Determine PIN status
        boolean pinSet = card.getPinHash() != null && !card.getPinHash().isEmpty();
        boolean pinLocked = card.getPinLockedUntil() != null 
                && card.getPinLockedUntil().isAfter(Instant.now());
        
        // Determine CVV status
        boolean cvvSet = card.getCvvHash() != null && !card.getCvvHash().isEmpty();

        return new CardResponse(
                card.getId(),
                card.getCustomerId(),
                card.getMaskedNumber(),
                card.getStatus(),
                card.getType(),
                card.getCurrency(),
                card.getSpendingLimit(),
                card.getCreatedAt(),
                card.getUpdatedAt(),
                card.getCancellationReason(),
                card.getDailyTransactionLimit(),
                card.getMonthlyTransactionLimit(),
                pinSet,
                pinLocked,
                card.getPinLockedUntil(),
                card.getFrozen() != null ? card.getFrozen() : false,
                card.getFrozenAt(),
                card.getFrozenReason(),
                card.getExpirationDate(),
                card.getIssuedAt(),
                card.getReplacedByCardId(),
                card.getReplacementReason(),
                card.getIsReplacement() != null ? card.getIsReplacement() : false,
                card.getAccountId(),
                cvvSet,
                card.getCvvGeneratedAt(),
                card.getCvvRotationDueDate(),
                card.getCardholderName(),
                card.getDailyAtmLimit(),
                card.getMonthlyAtmLimit(),
                card.getRenewedFromCardId(),
                card.getRenewalCount() != null ? card.getRenewalCount() : 0,
                card.getLastRenewedAt(),
                card.getContactlessEnabled() != null ? card.getContactlessEnabled() : true
        );
    }
}


