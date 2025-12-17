package com.banking.identity.service;

import com.banking.identity.domain.ApiKey;
import com.banking.identity.domain.ApiKey.ApiKeyStatus;
import com.banking.identity.repository.ApiKeyRepository;
import com.banking.identity.security.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String KEY_PREFIX = "bk_";

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, PasswordEncoder passwordEncoder) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ApiKeyCreationResult createApiKey(String name, String serviceName, String scopes, Integer rateLimit, Instant expiresAt, UUID createdBy) {
        // Generate secure API key
        byte[] keyBytes = new byte[32];
        SECURE_RANDOM.nextBytes(keyBytes);
        String rawKey = KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
        String keyPrefix = rawKey.substring(0, 10);

        // Hash the key for storage
        String keyHash = passwordEncoder.encode(rawKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setName(name);
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setServiceName(serviceName);
        apiKey.setScopes(scopes);
        apiKey.setRateLimit(rateLimit);
        apiKey.setExpiresAt(expiresAt);
        apiKey.setCreatedBy(createdBy);
        apiKey.setStatus(ApiKeyStatus.ACTIVE);

        apiKey = apiKeyRepository.save(apiKey);
        log.info("Created API key {} for service {}", apiKey.getId(), serviceName);

        // Return the raw key only once - it cannot be retrieved later
        return new ApiKeyCreationResult(apiKey.getId(), rawKey, keyPrefix);
    }

    public ApiKey validateApiKey(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(KEY_PREFIX)) {
            return null;
        }

        String prefix = rawKey.substring(0, Math.min(10, rawKey.length()));
        List<ApiKey> candidates = apiKeyRepository.findActiveByPrefix(prefix);

        for (ApiKey apiKey : candidates) {
            if (passwordEncoder.matches(rawKey, apiKey.getKeyHash())) {
                if (!apiKey.isValid()) {
                    return null;
                }
                // Update last used
                apiKey.setLastUsedAt(Instant.now());
                apiKeyRepository.save(apiKey);
                return apiKey;
            }
        }
        return null;
    }

    @Transactional
    public void revokeApiKey(UUID keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        apiKey.setStatus(ApiKeyStatus.REVOKED);
        apiKey.setRevokedAt(Instant.now());
        apiKeyRepository.save(apiKey);

        log.info("Revoked API key {}", keyId);
    }

    public List<ApiKey> getApiKeysByService(String serviceName) {
        return apiKeyRepository.findByServiceName(serviceName);
    }

    public List<ApiKey> getApiKeysByUser(UUID userId) {
        return apiKeyRepository.findByCreatedBy(userId);
    }

    public record ApiKeyCreationResult(UUID keyId, String rawKey, String keyPrefix) {}
}
