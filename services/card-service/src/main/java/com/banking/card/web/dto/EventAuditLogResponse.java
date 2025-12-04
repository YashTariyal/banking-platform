package com.banking.card.web.dto;

import com.banking.card.audit.EventDirection;
import com.banking.card.audit.EventStatus;
import java.time.Instant;
import java.util.UUID;

public record EventAuditLogResponse(
        UUID id,
        EventDirection direction,
        EventStatus status,
        String topic,
        String eventType,
        String eventKey,
        Integer recordPartition,
        Long recordOffset,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}


