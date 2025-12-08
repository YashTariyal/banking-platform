package com.banking.card.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class JwtHealthIndicator implements HealthIndicator {

    private final JwtDecoder jwtDecoder;
    private final boolean securityEnabled;

    public JwtHealthIndicator(JwtDecoder jwtDecoder,
                              @Value("${card.security.enabled:false}") boolean securityEnabled) {
        this.jwtDecoder = jwtDecoder;
        this.securityEnabled = securityEnabled;
    }

    @Override
    public Health health() {
        if (!securityEnabled) {
            return Health.up().withDetail("securityEnabled", false).build();
        }
        if (jwtDecoder != null) {
            return Health.up().withDetail("securityEnabled", true).build();
        }
        return Health.down().withDetail("securityEnabled", true).withDetail("error", "JwtDecoder missing").build();
    }
}


