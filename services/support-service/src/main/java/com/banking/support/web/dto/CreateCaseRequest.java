package com.banking.support.web.dto;

import com.banking.support.domain.CasePriority;
import com.banking.support.domain.CaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CreateCaseRequest(
        @NotNull(message = "Case type is required")
        CaseType caseType,

        @NotNull(message = "Priority is required")
        CasePriority priority,

        @NotNull(message = "Customer ID is required")
        UUID customerId,

        UUID accountId,

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description,

        Instant dueDate
) {
}

