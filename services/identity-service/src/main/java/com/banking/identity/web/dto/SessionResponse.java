package com.banking.identity.web.dto;

import com.banking.identity.domain.SessionStatus;
import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID userId,
        String deviceId,
        String userAgent,
        String ipAddress,
        SessionStatus status,
        Instant expiresAt,
        Instant lastUsedAt,
        Instant createdAt
) {
}

