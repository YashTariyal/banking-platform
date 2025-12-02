package com.banking.account.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and identity utilities")
public class AuthController {

    @GetMapping("/me")
    @Operation(
            summary = "Get current principal",
            description = "Returns information about the current authenticated principal based on the JWT."
    )
    public AuthInfoResponse me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        if (jwt == null) {
            return new AuthInfoResponse(null, null, List.of(), List.of());
        }

        List<String> scopes = new ArrayList<>();
        extractScopesFromClaim(jwt.getClaims().get("scope"), scopes);
        extractScopesFromClaim(jwt.getClaims().get("scp"), scopes);
        scopes = scopes.stream().distinct().toList();

        List<String> authorities = authentication != null
                ? authentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .distinct()
                        .toList()
                : List.of();

        return new AuthInfoResponse(
                jwt.getSubject(),
                jwt.getIssuer() != null ? jwt.getIssuer().toString() : null,
                scopes,
                authorities
        );
    }

    private void extractScopesFromClaim(Object claim, List<String> accumulator) {
        if (claim instanceof String scopeString) {
            for (String scope : scopeString.split("\\s+")) {
                if (!scope.isBlank()) {
                    accumulator.add(scope);
                }
            }
        } else if (claim instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item instanceof String scope && !scope.isBlank()) {
                    accumulator.add(scope);
                }
            }
        }
    }
}


