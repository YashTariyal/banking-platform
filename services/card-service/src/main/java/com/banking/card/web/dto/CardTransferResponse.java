package com.banking.card.web.dto;

import com.banking.card.domain.TransferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CardTransferResponse(
        UUID id,
        UUID fromCardId,
        UUID toCardId,
        BigDecimal amount,
        String currency,
        TransferStatus status,
        Instant transferDate,
        String failureReason,
        Instant createdAt
) {
}

