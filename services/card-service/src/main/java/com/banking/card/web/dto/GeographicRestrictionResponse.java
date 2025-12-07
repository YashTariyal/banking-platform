package com.banking.card.web.dto;

import com.banking.card.domain.RestrictionAction;
import java.time.Instant;
import java.util.UUID;

public record GeographicRestrictionResponse(
        UUID id,
        UUID cardId,
        String countryCode,
        RestrictionAction action,
        Instant createdAt
) {
}

