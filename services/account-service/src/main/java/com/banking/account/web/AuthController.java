package com.banking.account.web;

import com.banking.account.security.RefreshTokenService;
import com.banking.account.security.RefreshTokenService.RefreshTokenInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication, identity utilities and demo refresh tokens")
public class AuthController {

    private final RefreshTokenService refreshTokenService;

    public AuthController(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current principal",
            description = "Returns information about the current authenticated principal based on the JWT."
    )
    public AuthInfoResponse me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        if (jwt == null) {
            return new AuthInfoResponse(null, null, List.of(), List.of());
        }

        List<String> scopes = extractScopes(jwt);

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

    @PostMapping("/refresh-token")
    @Operation(
            summary = "Issue a refresh token (demo helper)",
            description = """
                    Issues a long-lived opaque refresh token for the current subject and scopes.
                    This service still acts as a resource server — it does not perform username/password login.
                    The refresh token is stored in an in-memory cache and can be exchanged on /api/auth/refresh.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refresh token issued",
                    content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid access token")
    })
    public ResponseEntity<RefreshTokenResponse> issueRefreshToken(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<String> scopes = extractScopes(jwt);
        String scopeString = String.join(" ", scopes);
        RefreshTokenInfo info = refreshTokenService.issue(jwt.getSubject(), scopeString);
        RefreshTokenResponse response = new RefreshTokenResponse(info.token(), info.subject(), info.scope(), info.expiresAt());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access using an opaque refresh token (demo)",
            description = """
                    Validates an opaque refresh token that was previously issued by /api/auth/refresh-token.
                    This endpoint does NOT mint new JWT access tokens itself; in a real deployment your IdP remains the
                    source of truth for access_token and refresh_token. This is a demo helper showing how a refresh
                    token cache could work.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refresh token is valid"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody RefreshRequest request) {
        return refreshTokenService.validate(request.refreshToken())
                .map(info ->
                        // In a real system you would call the IdP here to mint a new JWT using the subject and scope.
                        ResponseEntity.<Map<String, Object>>ok(Map.of(
                                "subject", info.subject(),
                                "scope", info.scope(),
                                "refreshToken", info.token(),
                                "expiresAt", info.expiresAt()
                        ))
                )
                .orElseGet(() -> ResponseEntity.<Map<String, Object>>badRequest().body(Map.of(
                        "error", "invalid_refresh_token",
                        "error_description", "Refresh token is invalid or expired"
                )));
    }

    private List<String> extractScopes(Jwt jwt) {
        List<String> scopes = new ArrayList<>();
        extractScopesFromClaim(jwt.getClaims().get("scope"), scopes);
        extractScopesFromClaim(jwt.getClaims().get("scp"), scopes);
        scopes = scopes.stream().distinct().toList();
        return scopes;
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

    public record RefreshTokenResponse(
            String refreshToken,
            String subject,
            String scope,
            Instant expiresAt
    ) {
    }

    public record RefreshRequest(
            @Schema(description = "Opaque refresh token issued by /api/auth/refresh-token", required = true)
            String refreshToken
    ) {
    }
}

