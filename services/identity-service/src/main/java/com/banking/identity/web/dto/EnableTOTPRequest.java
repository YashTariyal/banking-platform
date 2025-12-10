package com.banking.identity.web.dto;

import jakarta.validation.constraints.NotBlank;

public record EnableTOTPRequest(
        @NotBlank String totpSecret
) {
}

