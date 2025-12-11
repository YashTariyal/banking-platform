package com.banking.payment.web.dto;

import com.banking.payment.domain.PaymentDirection;
import com.banking.payment.domain.PaymentRail;
import com.banking.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String referenceId,
        PaymentRail rail,
        PaymentDirection direction,
        PaymentStatus status,
        UUID fromAccountId,
        UUID toAccountId,
        String toExternalAccount,
        String toExternalRouting,
        String toExternalBankName,
        BigDecimal amount,
        String currency,
        String description,
        String failureReason,
        String externalReference,
        Instant initiatedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

