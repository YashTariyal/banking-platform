package com.banking.support.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectOverrideRequest(
        @NotBlank(message = "Rejection reason is required")
        @Size(max = 5000, message = "Rejection reason must not exceed 5000 characters")
        String rejectionReason
) {
}

