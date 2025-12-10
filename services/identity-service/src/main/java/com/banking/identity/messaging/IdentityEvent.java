package com.banking.identity.messaging;

import java.time.Instant;
import java.util.UUID;

public record IdentityEvent(
        UUID userId,
        String username,
        UUID customerId,
        String eventType,
        Instant occurredAt
) {
    public static IdentityEvent userRegistered(UUID userId, String username, UUID customerId) {
        return new IdentityEvent(userId, username, customerId, "USER_REGISTERED", Instant.now());
    }

    public static IdentityEvent userLoggedIn(UUID userId, String username, UUID customerId) {
        return new IdentityEvent(userId, username, customerId, "USER_LOGGED_IN", Instant.now());
    }

    public static IdentityEvent userLoggedOut(UUID userId) {
        return new IdentityEvent(userId, null, null, "USER_LOGGED_OUT", Instant.now());
    }

    public static IdentityEvent userLocked(UUID userId, String username, UUID customerId) {
        return new IdentityEvent(userId, username, customerId, "USER_LOCKED", Instant.now());
    }
}

