package com.banking.identity.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fail fast when security is enabled without a strong JWT signing key.
 */
@Component
public class SecurityStartupValidator {

    private final Environment environment;
    private final boolean securityEnabled;
    private final String jwtSecret;

    public SecurityStartupValidator(Environment environment,
                                    @Value("${identity.security.enabled:false}") boolean securityEnabled,
                                    @Value("${identity.jwt.secret-key:}") String jwtSecret) {
        this.environment = environment;
        this.securityEnabled = securityEnabled;
        this.jwtSecret = jwtSecret;
    }

    @PostConstruct
    public void validateSecurityConfiguration() {
        if (!securityEnabled) {
            return;
        }

        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException("""
                    identity.security.enabled=true but no JWT secret configured.
                    Please configure identity.jwt.secret-key with a 256-bit (>=32 char) random value.
                    """);
        }

        if (isDefaultOrWeak(jwtSecret)) {
            throw new IllegalStateException("""
                    identity.jwt.secret-key is using a default/weak value. Provide a unique 256-bit secret.
                    """);
        }
    }

    private boolean isDefaultOrWeak(String secret) {
        if (secret.length() < 32) {
            return true;
        }
        String defaultSecret = environment.getProperty(
                "identity.jwt.secret-key",
                "default-secret-key-change-in-production-min-256-bits");
        return secret.equals(defaultSecret)
                || secret.toLowerCase().contains("default-secret")
                || secret.toLowerCase().contains("change-in-production");
    }
}
