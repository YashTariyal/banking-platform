package com.banking.card.domain;

public enum FraudEventType {
    VELOCITY_EXCEEDED,
    UNUSUAL_AMOUNT,
    UNUSUAL_LOCATION,
    UNUSUAL_MERCHANT,
    MULTIPLE_DECLINES,
    PATTERN_ANOMALY
}

