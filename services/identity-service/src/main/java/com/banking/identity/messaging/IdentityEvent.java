package com.banking.identity.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record IdentityEvent(
        UUID userId,
        String username,
        UUID customerId,
        String eventType,
        Instant occurredAt,
        Map<String, String> metadata
) {
    public IdentityEvent(UUID userId, String username, UUID customerId, String eventType, Instant occurredAt) {
        this(userId, username, customerId, eventType, occurredAt, Map.of());
    }

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

    public static IdentityEvent passwordResetRequested(UUID userId, String email, String token) {
        return new IdentityEvent(userId, null, null, "PASSWORD_RESET_REQUESTED", Instant.now(),
                Map.of("email", email, "token", token));
    }

    public static IdentityEvent passwordChanged(UUID userId, String username) {
        return new IdentityEvent(userId, username, null, "PASSWORD_CHANGED", Instant.now());
    }

    public static IdentityEvent emailVerificationRequested(UUID userId, String email, String token) {
        return new IdentityEvent(userId, null, null, "EMAIL_VERIFICATION_REQUESTED", Instant.now(),
                Map.of("email", email, "token", token));
    }

    public static IdentityEvent emailVerified(UUID userId, String email) {
        return new IdentityEvent(userId, null, null, "EMAIL_VERIFIED", Instant.now(),
                Map.of("email", email));
    }
}

