package com.banking.card.web.dto;

import jakarta.validation.constraints.Size;

public record FreezeCardRequest(
        @Size(max = 255, message = "Freeze reason must not exceed 255 characters")
        String reason
) {
}

