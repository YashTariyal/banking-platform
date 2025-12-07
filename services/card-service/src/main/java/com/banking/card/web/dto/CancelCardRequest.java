package com.banking.card.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelCardRequest(
        @NotBlank(message = "Cancellation reason is required")
        @Size(max = 255, message = "Cancellation reason must not exceed 255 characters")
        String reason
) {
}

