package com.banking.kyc.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Custom JWT authentication converter that:
 * - Normalizes scopes from both space-separated and list-style "scope"/"scp" claims
 * - Maps common role claims (realm_access.roles, roles) to ROLE_ authorities
 */
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter scopesConverter;

    public JwtAuthConverter() {
        this.scopesConverter = new JwtGrantedAuthoritiesConverter();
        this.scopesConverter.setAuthorityPrefix("");
        this.scopesConverter.setAuthoritiesClaimName("scope");
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1) Scopes from "scope" (space-separated string or list)
        authorities.addAll(fromScopeClaims(jwt, "scope"));

        // 2) Scopes from "scp" (commonly used by some IdPs)
        authorities.addAll(fromScopeClaims(jwt, "scp"));

        // 3) Roles from realm_access.roles (Keycloak-style)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            Object rolesClaim = realmAccess.get("roles");
            if (rolesClaim instanceof Collection<?> roles) {
                for (Object role : roles) {
                    if (role instanceof String roleName && !roleName.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
                    }
                }
            }
        }

        // 4) Generic "roles" claim as list of strings
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            for (String role : roles) {
                if (role != null && !role.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
            }
        }

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private Collection<? extends GrantedAuthority> fromScopeClaims(Jwt jwt, String claimName) {
        Set<String> normalizedScopes = new HashSet<>();

        Object claim = jwt.getClaims().get(claimName);
        if (claim instanceof String scopeString) {
            for (String scope : scopeString.split("\\s+")) {
                if (!scope.isBlank()) {
                    normalizedScopes.add(scope);
                }
            }
        } else if (claim instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item instanceof String scope && !scope.isBlank()) {
                    normalizedScopes.add(scope);
                }
            }
        }

        return normalizedScopes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
