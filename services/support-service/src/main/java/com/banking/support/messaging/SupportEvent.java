package com.banking.support.messaging;

import com.banking.support.domain.CasePriority;
import com.banking.support.domain.CaseStatus;
import com.banking.support.domain.CaseType;
import com.banking.support.domain.OverrideStatus;
import com.banking.support.domain.OverrideType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SupportEvent(
        String eventType,
        UUID caseId,
        UUID overrideId,
        String caseNumber,
        CaseType caseType,
        CasePriority priority,
        CaseStatus caseStatus,
        OverrideType overrideType,
        OverrideStatus overrideStatus,
        UUID customerId,
        UUID accountId,
        UUID assignedTo,
        UUID createdBy,
        UUID requestedBy,
        UUID approvedBy,
        BigDecimal amount,
        String currency,
        Instant timestamp
) {
}

