package com.banking.support.web.dto;

import com.banking.support.domain.CasePriority;
import com.banking.support.domain.CaseStatus;
import com.banking.support.domain.CaseType;
import java.time.Instant;
import java.util.UUID;

public record SupportCaseResponse(
        UUID id,
        String caseNumber,
        CaseType caseType,
        CasePriority priority,
        CaseStatus status,
        UUID customerId,
        UUID accountId,
        String title,
        String description,
        UUID assignedTo,
        UUID createdBy,
        UUID resolvedBy,
        Instant resolvedAt,
        String resolutionNotes,
        Instant dueDate,
        Instant createdAt,
        Instant updatedAt
) {
}

