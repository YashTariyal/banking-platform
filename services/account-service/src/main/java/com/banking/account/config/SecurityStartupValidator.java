package com.banking.account.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class SecurityStartupValidator {

    private final Environment environment;
    private final boolean securityEnabled;

    public SecurityStartupValidator(Environment environment,
                                    @Value("${account.security.enabled:false}") boolean securityEnabled) {
        this.environment = environment;
        this.securityEnabled = securityEnabled;
    }

    @PostConstruct
    public void validateSecurityConfiguration() {
        if (!securityEnabled) {
            return;
        }

        String jwkSetUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");
        String secretKey = environment.getProperty("spring.security.oauth2.resourceserver.jwt.secret-key");

        if ((jwkSetUri == null || jwkSetUri.isBlank()) && (secretKey == null || secretKey.isBlank())) {
            throw new IllegalStateException("""
                    account.security.enabled=true but no JWT configuration found.
                    Please configure either:
                      - spring.security.oauth2.resourceserver.jwt.jwk-set-uri
                      - or spring.security.oauth2.resourceserver.jwt.secret-key
                    """);
        }
    }
}


