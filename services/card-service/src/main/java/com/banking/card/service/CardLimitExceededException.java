package com.banking.card.service;

import java.math.BigDecimal;

public class CardLimitExceededException extends CardOperationException {
    private final BigDecimal requestedAmount;
    private final BigDecimal availableLimit;

    public CardLimitExceededException(String message, BigDecimal requestedAmount, BigDecimal availableLimit) {
        super(message);
        this.requestedAmount = requestedAmount;
        this.availableLimit = availableLimit;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public BigDecimal getAvailableLimit() {
        return availableLimit;
    }
}

