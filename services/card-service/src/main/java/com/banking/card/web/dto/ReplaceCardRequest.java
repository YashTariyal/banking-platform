package com.banking.card.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplaceCardRequest(
        @NotBlank(message = "Replacement reason is required")
        @Size(max = 255, message = "Replacement reason must not exceed 255 characters")
        String reason
) {
}

