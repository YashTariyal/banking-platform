package com.banking.payment.messaging;

import com.banking.payment.domain.PaymentDirection;
import com.banking.payment.domain.PaymentRail;
import com.banking.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentEvent(
        String eventType,
        UUID paymentId,
        String referenceId,
        PaymentRail rail,
        PaymentDirection direction,
        PaymentStatus status,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        String description,
        String failureReason,
        Instant timestamp
) {
}

