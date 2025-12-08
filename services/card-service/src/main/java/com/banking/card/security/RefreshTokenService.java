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
    public RefreshTokenInfo issue(String subject, String scope, String deviceId, String userAgent) {
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
        entity.setDeviceId(deviceId);
        entity.setUserAgent(userAgent);

        repository.save(entity);
        return toInfo(entity);
    }

    /**
     * Validates a refresh token and, if valid and not expired or revoked, returns its info.
     * Expired tokens are marked as revoked to prevent reuse.
     */
    @Transactional
    public Optional<RefreshTokenInfo> validate(String token) {
        return validate(token, null, null);
    }

    /**
     * Validates a refresh token with device binding check.
     */
    @Transactional
    public Optional<RefreshTokenInfo> validate(String token, String deviceId, String userAgent) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        String tokenValue = Objects.requireNonNull(token);

        return repository.findById(tokenValue)
                .filter(rt -> {
                    if (rt.isRevoked()) {
                        handleReuse(rt);
                        return false;
                    }
                    if (Instant.now().isAfter(rt.getExpiresAt())) {
                        // Mark as revoked on next write transaction to prevent reuse
                        markExpired(rt);
                        return false;
                    }
                    if (rt.getDeviceId() != null && deviceId != null && !rt.getDeviceId().equals(deviceId)) {
                        // Device mismatch -> revoke all tokens for subject
                        revokeAllForSubject(rt.getSubject(), "device-mismatch");
                        return false;
                    }
                    if (rt.getUserAgent() != null && userAgent != null && !rt.getUserAgent().equals(userAgent)) {
                        revokeAllForSubject(rt.getSubject(), "device-mismatch");
                        return false;
                    }
                    rt.setLastUsedAt(Instant.now());
                    repository.save(rt);
                    return true;
                })
                .map(this::toInfo);
    }

    /**
     * Rotates a refresh token: revokes the old token and issues a new one preserving subject/scope/device binding.
     */
    @Transactional
    public Optional<RefreshTokenInfo> rotate(String token, String deviceId, String userAgent) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        String tokenValue = Objects.requireNonNull(token);
        return repository.findById(tokenValue)
                .filter(rt -> !rt.isRevoked() && !Instant.now().isAfter(rt.getExpiresAt()))
                .flatMap(rt -> {
                    revokeWithReason(rt, "rotated");
                    return Optional.of(issue(rt.getSubject(), rt.getScope(), deviceId, userAgent));
                });
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
            revokeWithReason(rt, "revoked");
        });
    }

    private void markExpired(RefreshToken token) {
        revokeWithReason(token, "expired");
        token.setLastUsedAt(Instant.now());
    }

    private void handleReuse(RefreshToken token) {
        // On reuse of a revoked token, revoke all tokens for the subject as a safety measure.
        revokeAllForSubject(token.getSubject(), "reuse-detected");
    }

    private void revokeAllForSubject(String subject, String reason) {
        repository.findBySubject(subject).forEach(rt -> {
            revokeWithReason(rt, reason);
        });
    }

    private void revokeWithReason(RefreshToken token, String reason) {
        token.setRevoked(true);
        token.setRevocationReason(reason);
        repository.save(token);
    }

    private RefreshTokenInfo toInfo(RefreshToken token) {
        return new RefreshTokenInfo(
                Objects.requireNonNull(token.getToken(), "token"),
                Objects.requireNonNull(token.getSubject(), "subject"),
                token.getScope(),
                Objects.requireNonNull(token.getIssuedAt(), "issuedAt"),
                Objects.requireNonNull(token.getExpiresAt(), "expiresAt"),
                token.isRevoked(),
                token.getRevocationReason(),
                token.getDeviceId(),
                token.getUserAgent(),
                token.getLastUsedAt()
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
            boolean revoked,
            String revocationReason,
            String deviceId,
            String userAgent,
            Instant lastUsedAt
    ) {
    }
}
