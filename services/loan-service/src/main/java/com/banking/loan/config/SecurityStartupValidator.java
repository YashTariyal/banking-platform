package com.banking.loan.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fail fast if loan-service is started with security enabled but
 * without any JWT resource server configuration.
 */
@Component
public class SecurityStartupValidator {

    private final Environment environment;
    private final boolean securityEnabled;

    public SecurityStartupValidator(Environment environment,
                                    @Value("${loan.security.enabled:false}") boolean securityEnabled) {
        this.environment = environment;
        this.securityEnabled = securityEnabled;
    }

    @PostConstruct
    public void validateSecurityConfiguration() {
        if (!securityEnabled) {
            return;
        }

        String jwkSetUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");
        String issuerUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
        String secretKey = environment.getProperty("spring.security.oauth2.resourceserver.jwt.secret-key");

        if ((jwkSetUri == null || jwkSetUri.isBlank())
                && (issuerUri == null || issuerUri.isBlank())
                && (secretKey == null || secretKey.isBlank())) {
            throw new IllegalStateException("""
                    loan.security.enabled=true but no JWT configuration found.
                    Please configure at least one of:
                      - spring.security.oauth2.resourceserver.jwt.jwk-set-uri
                      - spring.security.oauth2.resourceserver.jwt.issuer-uri
                      - spring.security.oauth2.resourceserver.jwt.secret-key
                    """);
        }
    }
}

