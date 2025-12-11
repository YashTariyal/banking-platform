package com.banking.transaction.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FailTransactionRequest(
        @NotBlank(message = "Failure reason is required")
        @Size(max = 1000, message = "Failure reason must not exceed 1000 characters")
        String failureReason
) {
}

