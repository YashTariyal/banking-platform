package com.banking.account.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Simple in-memory refresh token store.
 *
 * In a real production environment you would back this with Redis or another shared
 * store so that refresh tokens survive restarts and are visible on all instances.
 */
@Service
public class RefreshTokenService {

    private final Duration refreshTokenTtl;

    private final Map<String, RefreshTokenInfo> store = new ConcurrentHashMap<>();

    public RefreshTokenService(
            @Value("${account.security.refresh-token.ttl-seconds:2592000}") long ttlSeconds // 30 days default
    ) {
        this.refreshTokenTtl = Duration.ofSeconds(ttlSeconds);
    }

    /**
     * Issues a new opaque refresh token for the given subject and scope representation.
     */
    public RefreshTokenInfo issue(String subject, String scope) {
        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("subject must not be empty when issuing refresh token");
        }
        String token = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(refreshTokenTtl);
        RefreshTokenInfo info = new RefreshTokenInfo(token, subject, scope, now, expiresAt);
        store.put(token, info);
        return info;
    }

    /**
     * Validates a refresh token and, if valid and not expired, returns its info.
     */
    public Optional<RefreshTokenInfo> validate(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        RefreshTokenInfo info = store.get(token);
        if (info == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(info.expiresAt())) {
            store.remove(token);
            return Optional.empty();
        }
        return Optional.of(info);
    }

    /**
     * Revokes a refresh token so it can no longer be used.
     */
    public void revoke(String token) {
        if (StringUtils.hasText(token)) {
            store.remove(token);
        }
    }

    /**
     * Represents refresh token metadata stored in the cache.
     */
    public record RefreshTokenInfo(
            String token,
            String subject,
            String scope,
            Instant issuedAt,
            Instant expiresAt
    ) {
    }
}


