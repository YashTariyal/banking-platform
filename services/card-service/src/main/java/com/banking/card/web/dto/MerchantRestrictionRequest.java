package com.banking.card.web.dto;

import com.banking.card.domain.RestrictionAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MerchantRestrictionRequest(
        @NotBlank(message = "Merchant category code is required")
        @Size(max = 10, message = "MCC must not exceed 10 characters")
        String merchantCategoryCode,
        @NotNull(message = "Action is required")
        RestrictionAction action
) {
}

