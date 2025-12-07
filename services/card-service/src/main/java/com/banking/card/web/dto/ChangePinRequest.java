package com.banking.card.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePinRequest(
        @NotBlank(message = "Current PIN is required")
        @Size(min = 4, max = 6, message = "PIN must be between 4 and 6 digits")
        @Pattern(regexp = "\\d+", message = "PIN must contain only digits")
        String currentPin,
        @NotBlank(message = "New PIN is required")
        @Size(min = 4, max = 6, message = "PIN must be between 4 and 6 digits")
        @Pattern(regexp = "\\d+", message = "PIN must contain only digits")
        String newPin
) {
}

