package com.banking.card.service;

public class CardRestrictionViolationException extends CardOperationException {
    private final String restrictionType;
    private final String restrictionValue;

    public CardRestrictionViolationException(String message, String restrictionType, String restrictionValue) {
        super(message);
        this.restrictionType = restrictionType;
        this.restrictionValue = restrictionValue;
    }

    public String getRestrictionType() {
        return restrictionType;
    }

    public String getRestrictionValue() {
        return restrictionValue;
    }
}

