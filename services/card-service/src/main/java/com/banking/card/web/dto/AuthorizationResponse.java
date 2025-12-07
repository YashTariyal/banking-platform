package com.banking.card.web.dto;

import com.banking.card.domain.AuthorizationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AuthorizationResponse(
        UUID id,
        UUID cardId,
        BigDecimal amount,
        String currency,
        AuthorizationStatus status,
        String declineReason,
        Instant checkedAt
) {
}

