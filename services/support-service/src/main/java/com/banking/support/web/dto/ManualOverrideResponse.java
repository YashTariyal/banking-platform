package com.banking.support.web.dto;

import com.banking.support.domain.OverrideStatus;
import com.banking.support.domain.OverrideType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ManualOverrideResponse(
        UUID id,
        OverrideType overrideType,
        OverrideStatus status,
        UUID customerId,
        UUID accountId,
        UUID entityId,
        UUID requestedBy,
        UUID approvedBy,
        UUID rejectedBy,
        String reason,
        String overrideValue,
        BigDecimal amount,
        String currency,
        Instant expiresAt,
        Instant approvedAt,
        Instant rejectedAt,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
}

