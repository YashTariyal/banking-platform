package com.banking.identity.web.dto;

import jakarta.validation.constraints.NotBlank;

public record EnableSMSRequest(
        @NotBlank String phoneNumber
) {
}

