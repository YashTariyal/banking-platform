package com.banking.risk.web.dto;

import com.banking.risk.domain.AlertStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateAlertStatusRequest(
        @NotNull(message = "Status is required")
        AlertStatus status,

        UUID reviewedBy,

        @Size(max = 1000, message = "Resolution notes must not exceed 1000 characters")
        String resolutionNotes
) {
}

