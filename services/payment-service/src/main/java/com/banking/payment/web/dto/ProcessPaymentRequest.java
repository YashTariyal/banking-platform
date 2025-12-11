package com.banking.payment.web.dto;

import jakarta.validation.constraints.Size;

public record ProcessPaymentRequest(
        @Size(max = 255, message = "External reference must not exceed 255 characters")
        String externalReference
) {
}

