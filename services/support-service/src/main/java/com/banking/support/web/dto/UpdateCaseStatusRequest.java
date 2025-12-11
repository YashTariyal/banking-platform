package com.banking.support.web.dto;

import com.banking.support.domain.CaseStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCaseStatusRequest(
        @NotNull(message = "Status is required")
        CaseStatus status
) {
}

