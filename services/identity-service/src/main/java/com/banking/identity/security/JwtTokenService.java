package com.banking.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final SecretKey secretKey;
    private final long accessTokenValiditySeconds;
    private final long refreshTokenValiditySeconds;
    private final String issuer;

    public JwtTokenService(
            @Value("${identity.jwt.secret-key:default-secret-key-change-in-production-min-256-bits}") String secretKey,
            @Value("${identity.jwt.access-token-validity-seconds:3600}") long accessTokenValiditySeconds,
            @Value("${identity.jwt.refresh-token-validity-seconds:86400}") long refreshTokenValiditySeconds,
            @Value("${identity.jwt.issuer:identity-service}") String issuer
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
        this.issuer = issuer;
    }

    public String generateAccessToken(UUID userId, String username, UUID customerId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("customerId", customerId.toString());
        claims.put("username", username);
        claims.put("type", "access");

        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(accessTokenValiditySeconds);

        return Jwts.builder()
                .claims(claims)
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("type", "refresh");

        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(refreshTokenValiditySeconds);

        return Jwts.builder()
                .claims(claims)
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token: " + e.getMessage());
        }
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public boolean isAccessToken(String token) {
        try {
            Claims claims = validateToken(token);
            return "access".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = validateToken(token);
            return "refresh".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }
}

