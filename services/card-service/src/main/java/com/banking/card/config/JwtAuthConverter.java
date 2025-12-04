package com.banking.card.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Minimal JWT converter for card-service:
 * - Normalizes scopes from both string and list style "scope"/"scp" claims.
 * - Does not add role mappings; card-service uses flat authorities like "cards.read".
 */
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        authorities.addAll(fromScopeClaims(jwt, "scope"));
        authorities.addAll(fromScopeClaims(jwt, "scp"));

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


