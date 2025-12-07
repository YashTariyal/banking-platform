package com.banking.card.web.dto;

import com.banking.card.domain.NotificationChannel;
import com.banking.card.domain.NotificationStatus;
import com.banking.card.domain.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID cardId,
        NotificationType notificationType,
        NotificationChannel channel,
        String subject,
        String message,
        NotificationStatus status,
        Instant sentAt,
        Instant createdAt
) {
}

