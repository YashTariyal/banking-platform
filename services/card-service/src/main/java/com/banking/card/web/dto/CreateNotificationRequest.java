package com.banking.card.web.dto;

import com.banking.card.domain.NotificationChannel;
import com.banking.card.domain.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(
        @NotNull(message = "Notification type is required")
        NotificationType notificationType,
        @NotNull(message = "Channel is required")
        NotificationChannel channel,
        @Size(max = 255) String subject,
        @NotBlank(message = "Message is required")
        String message
) {
}

