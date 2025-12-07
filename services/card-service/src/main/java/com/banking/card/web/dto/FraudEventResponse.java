package com.banking.card.web.dto;

import com.banking.card.domain.FraudEventType;
import com.banking.card.domain.FraudSeverity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudEventResponse(
        UUID id,
        UUID cardId,
        FraudEventType eventType,
        FraudSeverity severity,
        String description,
        UUID transactionId,
        BigDecimal fraudScore,
        Instant detectedAt,
        Boolean resolved
) {
}

