package com.banking.identity.web.dto;

import com.banking.identity.domain.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        UUID customerId,
        UserStatus status,
        Boolean emailVerified,
        Instant emailVerifiedAt,
        Instant lastLoginAt,
        Instant createdAt
) {
}

