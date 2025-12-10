package com.banking.identity.web;

import com.banking.identity.domain.User;
import com.banking.identity.service.AuthenticationService;
import com.banking.identity.service.SessionService;
import com.banking.identity.web.dto.LoginRequest;
import com.banking.identity.web.dto.LoginResponse;
import com.banking.identity.web.dto.RefreshTokenRequest;
import com.banking.identity.web.dto.RefreshTokenResponse;
import com.banking.identity.web.dto.RegisterRequest;
import com.banking.identity.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final SessionService sessionService;

    public AuthController(AuthenticationService authenticationService, SessionService sessionService) {
        this.authenticationService = authenticationService;
        this.sessionService = sessionService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account linked to a customer"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or username/email already exists")
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponse(user));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Authenticates a user and returns access and refresh tokens"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials"),
            @ApiResponse(responseCode = "423", description = "Account is locked")
    })
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest httpRequest
    ) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        LoginResponse response = authenticationService.login(request, deviceId, userAgent, ipAddress);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Logout",
            description = "Logs out the current session by invalidating the refresh token"
    )
    public void logout(@RequestBody RefreshTokenRequest request) {
        authenticationService.logout(request.refreshToken());
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Generates a new access token using a valid refresh token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid refresh token")
    })
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String newAccessToken = sessionService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(new RefreshTokenResponse(newAccessToken, request.refreshToken()));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCustomerId(),
                user.getStatus(),
                user.getEmailVerified(),
                user.getEmailVerifiedAt(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}

