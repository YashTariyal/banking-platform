package com.banking.card.web.dto;

import com.banking.card.domain.RestrictionAction;
import java.time.Instant;
import java.util.UUID;

public record MerchantRestrictionResponse(
        UUID id,
        UUID cardId,
        String merchantCategoryCode,
        RestrictionAction action,
        Instant createdAt
) {
}

