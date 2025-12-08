package com.banking.card.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jwt.SignedJWT;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
@EnableMethodSecurity(jsr250Enabled = true, prePostEnabled = true)
@EnableConfigurationProperties(JwtKeyProperties.class)
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
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri,
            JwtKeyProperties jwtKeyProperties) {
        
        // If jwk-set-uri is provided, use it
        if (StringUtils.hasText(jwkSetUri)) {
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        }
        
        // If issuer-uri is provided, use it (Spring Boot will auto-configure from issuer-uri)
        if (StringUtils.hasText(issuerUri)) {
            return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        }

        // If multiple symmetric keys provided with kid support, build JWKSet
        if (jwtKeyProperties.getKeys() != null && !jwtKeyProperties.getKeys().isEmpty()) {
            List<JWK> jwks = jwtKeyProperties.getKeys().stream()
                    .filter(k -> StringUtils.hasText(k.getKid()) && StringUtils.hasText(k.getSecret()))
                    .map(k -> {
                        byte[] keyBytes = Base64.getDecoder().decode(k.getSecret());
                        return new OctetSequenceKey.Builder(keyBytes)
                                .algorithm(com.nimbusds.jose.JWSAlgorithm.HS256)
                                .keyID(k.getKid())
                                .build();
                    })
                    .collect(Collectors.toList());

            if (!jwks.isEmpty()) {
                Map<String, SecretKey> keyMap = jwks.stream()
                        .collect(Collectors.toMap(JWK::getKeyID, k -> new SecretKeySpec(k.toOctetSequenceKey().toByteArray(), "HmacSHA256")));
                SecretKey defaultKey = keyMap.values().iterator().next();
                return token -> {
                    String kid = extractKid(token);
                    SecretKey keyToUse = kid != null && keyMap.containsKey(kid) ? keyMap.get(kid) : defaultKey;
                    if (keyToUse == null) {
                        throw new JwtException("No matching key for kid " + kid);
                    }
                    return NimbusJwtDecoder.withSecretKey(keyToUse).build().decode(token);
                };
            }
        }

        // If secret-key is provided, use it for symmetric key (HS256)
        if (StringUtils.hasText(secretKey)) {
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

    private String extractKid(String token) {
        try {
            return SignedJWT.parse(token).getHeader().getKeyID();
        } catch (Exception e) {
            throw new JwtException("Unable to parse token header", e);
        }
    }
}


