package com.banking.identity.web;

import com.banking.identity.domain.ApiKey;
import com.banking.identity.service.ApiKeyService;
import com.banking.identity.service.ApiKeyService.ApiKeyCreationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/apikeys")
@Tag(name = "API Keys", description = "API key management for M2M authentication")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @Operation(summary = "Create a new API key")
    public ResponseEntity<ApiKeyResponse> createApiKey(@Valid @RequestBody CreateApiKeyRequest request) {
        ApiKeyCreationResult result = apiKeyService.createApiKey(
                request.name(),
                request.serviceName(),
                request.scopes(),
                request.rateLimit(),
                request.expiresAt(),
                request.createdBy()
        );
        return ResponseEntity.ok(new ApiKeyResponse(result.keyId(), result.rawKey(), result.keyPrefix(),
                "Store this key securely. It will not be shown again."));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revoke an API key")
    public ResponseEntity<Void> revokeApiKey(@PathVariable UUID id) {
        apiKeyService.revokeApiKey(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/service/{serviceName}")
    @Operation(summary = "List API keys for a service")
    public ResponseEntity<List<ApiKeyInfo>> getApiKeysByService(@PathVariable String serviceName) {
        List<ApiKeyInfo> keys = apiKeyService.getApiKeysByService(serviceName).stream()
                .map(this::toApiKeyInfo)
                .toList();
        return ResponseEntity.ok(keys);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List API keys created by user")
    public ResponseEntity<List<ApiKeyInfo>> getApiKeysByUser(@PathVariable UUID userId) {
        List<ApiKeyInfo> keys = apiKeyService.getApiKeysByUser(userId).stream()
                .map(this::toApiKeyInfo)
                .toList();
        return ResponseEntity.ok(keys);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate an API key")
    public ResponseEntity<ApiKeyValidationResponse> validateApiKey(@RequestBody ValidateApiKeyRequest request) {
        ApiKey apiKey = apiKeyService.validateApiKey(request.apiKey());
        if (apiKey == null) {
            return ResponseEntity.ok(new ApiKeyValidationResponse(false, null, null, null));
        }
        return ResponseEntity.ok(new ApiKeyValidationResponse(
                true, apiKey.getServiceName(), apiKey.getScopes(), apiKey.getRateLimit()));
    }

    private ApiKeyInfo toApiKeyInfo(ApiKey apiKey) {
        return new ApiKeyInfo(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getKeyPrefix() + "...",
                apiKey.getServiceName(),
                apiKey.getScopes(),
                apiKey.getStatus().name(),
                apiKey.getLastUsedAt(),
                apiKey.getExpiresAt(),
                apiKey.getCreatedAt()
        );
    }

    public record CreateApiKeyRequest(
            @NotBlank String name,
            @NotBlank String serviceName,
            String scopes,
            Integer rateLimit,
            Instant expiresAt,
            UUID createdBy
    ) {}

    public record ApiKeyResponse(
            UUID keyId,
            String apiKey,
            String keyPrefix,
            String warning
    ) {}

    public record ApiKeyInfo(
            UUID id,
            String name,
            String keyPrefix,
            String serviceName,
            String scopes,
            String status,
            Instant lastUsedAt,
            Instant expiresAt,
            Instant createdAt
    ) {}

    public record ValidateApiKeyRequest(String apiKey) {}

    public record ApiKeyValidationResponse(
            boolean valid,
            String serviceName,
            String scopes,
            Integer rateLimit
    ) {}
}
