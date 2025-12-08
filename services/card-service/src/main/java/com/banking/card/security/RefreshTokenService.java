package com.banking.card.security;

import com.banking.card.domain.RefreshToken;
import com.banking.card.repository.RefreshTokenRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Persistent refresh token store backed by the database.
 *
 * Tokens survive service restarts and are shared across instances.
 */
@Service
public class RefreshTokenService {

    private final Duration refreshTokenTtl;
    private final RefreshTokenRepository repository;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            @Value("${card.security.refresh-token.ttl-seconds:2592000}") long ttlSeconds // 30 days default
    ) {
        this.repository = repository;
        this.refreshTokenTtl = Duration.ofSeconds(ttlSeconds);
    }

    /**
     * Issues a new opaque refresh token for the given subject and scope representation.
     */
    @Transactional
    public RefreshTokenInfo issue(String subject, String scope) {
        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("subject must not be empty when issuing refresh token");
        }

        String token = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(refreshTokenTtl);

        RefreshToken entity = new RefreshToken();
        entity.setToken(token);
        entity.setSubject(subject);
        entity.setScope(scope);
        entity.setIssuedAt(now);
        entity.setExpiresAt(expiresAt);
        entity.setRevoked(false);

        repository.save(entity);
        return toInfo(entity);
    }

    /**
     * Validates a refresh token and, if valid and not expired or revoked, returns its info.
     * Expired tokens are marked as revoked to prevent reuse.
     */
    @Transactional
    public Optional<RefreshTokenInfo> validate(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        String tokenValue = Objects.requireNonNull(token);

        return repository.findById(tokenValue)
                .filter(rt -> {
                    if (rt.isRevoked()) {
                        return false;
                    }
                    if (Instant.now().isAfter(rt.getExpiresAt())) {
                        // Mark as revoked on next write transaction to prevent reuse
                        markExpired(rt);
                        return false;
                    }
                    return true;
                })
                .map(this::toInfo);
    }

    /**
     * Revokes a refresh token so it can no longer be used.
     */
    @Transactional
    public void revoke(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        String tokenValue = Objects.requireNonNull(token);
        repository.findById(tokenValue).ifPresent(rt -> {
            rt.setRevoked(true);
            repository.save(rt);
        });
    }

    private void markExpired(RefreshToken token) {
        token.setRevoked(true);
        repository.save(token);
    }

    private RefreshTokenInfo toInfo(RefreshToken token) {
        return new RefreshTokenInfo(
                Objects.requireNonNull(token.getToken(), "token"),
                Objects.requireNonNull(token.getSubject(), "subject"),
                token.getScope(),
                Objects.requireNonNull(token.getIssuedAt(), "issuedAt"),
                Objects.requireNonNull(token.getExpiresAt(), "expiresAt"),
                token.isRevoked()
        );
    }

    /**
     * Represents refresh token metadata stored in the persistent store.
     */
    public record RefreshTokenInfo(
            @NonNull String token,
            @NonNull String subject,
            String scope,
            @NonNull Instant issuedAt,
            @NonNull Instant expiresAt,
            boolean revoked
    ) {
    }
}
