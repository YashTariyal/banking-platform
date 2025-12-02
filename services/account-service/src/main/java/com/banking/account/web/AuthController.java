package com.banking.account.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
    public AuthInfoResponse me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return new AuthInfoResponse(null, null, List.of(), List.of());
        }

        List<String> scopes = jwt.getClaimAsStringList("scope");
        if (scopes == null) {
            scopes = jwt.getClaimAsStringList("scp");
        }
        if (scopes == null) {
            scopes = List.of();
        }

        return new AuthInfoResponse(
                jwt.getSubject(),
                jwt.getIssuer() != null ? jwt.getIssuer().toString() : null,
                scopes,
                List.of()
        );
    }
}


