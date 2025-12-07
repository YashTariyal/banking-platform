package com.banking.card.web;

import com.banking.card.security.RefreshTokenService;
import com.banking.card.security.RefreshTokenService.RefreshTokenInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
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
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthController(
            RefreshTokenService refreshTokenService,
            @Value("${card.security.cookie.secure:true}") boolean cookieSecure,
            @Value("${card.security.cookie.same-site:Lax}") String cookieSameSite) {
        this.refreshTokenService = refreshTokenService;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
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
                    The refresh token is also set as an HTTP-only cookie.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refresh token issued",
                    content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid access token")
    })
    public ResponseEntity<RefreshTokenResponse> issueRefreshToken(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletResponse response) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<String> scopes = extractScopes(jwt);
        String scopeString = String.join(" ", scopes);
        RefreshTokenInfo info = refreshTokenService.issue(jwt.getSubject(), scopeString);
        
        // Set refresh token in HTTP-only cookie
        setRefreshTokenCookie(response, info.token(), info.expiresAt());
        
        RefreshTokenResponse responseBody = new RefreshTokenResponse(
                info.token(), 
                info.subject(), 
                info.scope(), 
                info.expiresAt()
        );
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access using an opaque refresh token (demo)",
            description = """
                    Validates an opaque refresh token that was previously issued by /api/auth/refresh-token.
                    The refresh token can be provided either in the request body or as an HTTP-only cookie.
                    This endpoint does NOT mint new JWT access tokens itself; in a real deployment your IdP remains the
                    source of truth for access_token and refresh_token. This is a demo helper showing how a refresh
                    token cache could work.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refresh token is valid"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<Map<String, Object>> refresh(
            @RequestBody(required = false) RefreshRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        
        // Try to get refresh token from cookie first, then from request body
        String refreshToken = getRefreshTokenFromCookie(httpRequest);
        if (refreshToken == null && request != null) {
            refreshToken = request.refreshToken();
        }
        
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "invalid_request",
                    "error_description", "Refresh token is required"
            ));
        }
        
        return refreshTokenService.validate(refreshToken)
                .map(info -> {
                    // Refresh the cookie
                    setRefreshTokenCookie(httpResponse, info.token(), info.expiresAt());
                    
                    // In a real system you would call the IdP here to mint a new JWT using the subject and scope.
                    Map<String, Object> responseBody = new HashMap<>();
                    responseBody.put("subject", info.subject());
                    responseBody.put("scope", info.scope());
                    responseBody.put("refreshToken", info.token());
                    responseBody.put("expiresAt", info.expiresAt());
                    return ResponseEntity.ok(responseBody);
                })
                .orElseGet(() -> ResponseEntity.badRequest().body(Map.of(
                        "error", "invalid_refresh_token",
                        "error_description", "Refresh token is invalid or expired"
                )));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout and revoke refresh token",
            description = "Revokes the refresh token and clears the refresh token cookie."
    )
    public ResponseEntity<Map<String, Object>> logout(
            jakarta.servlet.http.HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        
        String refreshToken = getRefreshTokenFromCookie(httpRequest);
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }
        
        // Clear the cookie
        clearRefreshTokenCookie(httpResponse);
        
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token, Instant expiresAt) {
        // Calculate maxAge using Duration to preserve sub-second precision
        Duration duration = Duration.between(Instant.now(), expiresAt);
        
        // If the token has already expired (negative duration), delete the cookie immediately
        if (duration.isNegative() || duration.isZero()) {
            clearRefreshTokenCookie(response);
            return;
        }
        
        long maxAgeSeconds = duration.getSeconds();
        
        // If there are any nanoseconds remaining, round up to the next second
        // This prevents maxAge=0 which would cause the browser to immediately delete the cookie
        // Only do this for positive durations (non-expired tokens)
        if (duration.getNano() > 0) {
            maxAgeSeconds = maxAgeSeconds + 1;
        }
        
        // Ensure at least 1 second for non-expired tokens
        if (maxAgeSeconds <= 0) {
            maxAgeSeconds = 1;
        }
        
        Cookie cookie = new Cookie("refresh_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge((int) maxAgeSeconds);
        
        // Set SameSite attribute via Set-Cookie header
        String cookieHeader = String.format("refresh_token=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=%s",
                token, maxAgeSeconds, cookieSameSite);
        if (cookieSecure) {
            cookieHeader += "; Secure";
        }
        response.addHeader("Set-Cookie", cookieHeader);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String getRefreshTokenFromCookie(jakarta.servlet.http.HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
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
            @Schema(description = "Opaque refresh token issued by /api/auth/refresh-token")
            String refreshToken
    ) {
    }
}
