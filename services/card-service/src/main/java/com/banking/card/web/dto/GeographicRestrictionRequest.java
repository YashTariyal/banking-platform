package com.banking.card.web.dto;

import com.banking.card.domain.RestrictionAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GeographicRestrictionRequest(
        @NotBlank(message = "Country code is required")
        @Size(min = 2, max = 2, message = "Country code must be 2 characters (ISO 3166-1 alpha-2)")
        String countryCode,
        @NotNull(message = "Action is required")
        RestrictionAction action
) {
}

