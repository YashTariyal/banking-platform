package com.banking.card.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableMethodSecurity(jsr250Enabled = true, prePostEnabled = true)
public class SecurityConfig {

    @Value("${card.security.enabled:false}")
    private boolean securityEnabled;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!securityEnabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        JwtAuthConverter jwtAuthConverter = new JwtAuthConverter();

        http.authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api-docs/**"
                        ).permitAll()
                        // Auth endpoints - require authentication
                        .requestMatchers("/api/auth/refresh-token").authenticated()
                        .requestMatchers("/api/auth/me").authenticated()
                        // Allow refresh and logout with refresh token only (no access token required)
                        .requestMatchers("/api/auth/refresh", "/api/auth/logout").permitAll()
                        // All other API endpoints require authentication
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthConverter))
                );

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(name = "card.security.enabled", havingValue = "true")
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.secret-key:}") String secretKey,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri) {
        
        // If jwk-set-uri is provided, use it
        if (StringUtils.hasText(jwkSetUri)) {
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        }
        
        // If issuer-uri is provided, use it (Spring Boot will auto-configure from issuer-uri)
        if (StringUtils.hasText(issuerUri)) {
            return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        }
        
        // If secret-key is provided, use it for symmetric key (HS256)
        if (StringUtils.hasText(secretKey)) {
            // The secret-key in config is base64 encoded
            // Decode it to get the UTF-8 bytes of the secret string
            byte[] keyBytes = Base64.getDecoder().decode(secretKey);
            SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
            return NimbusJwtDecoder.withSecretKey(key).build();
        }
        
        // Fallback: create a decoder that will fail at runtime if no config is provided
        // This should not happen if SecurityStartupValidator is working correctly
        throw new IllegalStateException(
                "No JWT configuration found. Please configure either " +
                "spring.security.oauth2.resourceserver.jwt.secret-key, " +
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri, or " +
                "spring.security.oauth2.resourceserver.jwt.issuer-uri");
    }
}


