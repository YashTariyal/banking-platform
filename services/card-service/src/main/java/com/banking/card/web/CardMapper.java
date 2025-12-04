package com.banking.card.web;

import com.banking.card.domain.Card;
import com.banking.card.web.dto.CardResponse;

public final class CardMapper {

    private CardMapper() {
    }

    public static CardResponse toResponse(Card card) {
        return new CardResponse(
                card.getId(),
                card.getCustomerId(),
                card.getMaskedNumber(),
                card.getStatus(),
                card.getType(),
                card.getCurrency(),
                card.getSpendingLimit(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }
}


