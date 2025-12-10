package com.banking.identity.service;

import com.banking.identity.domain.Session;
import com.banking.identity.domain.SessionStatus;
import com.banking.identity.domain.User;
import com.banking.identity.domain.UserStatus;
import com.banking.identity.messaging.IdentityEventPublisher;
import com.banking.identity.repository.SessionRepository;
import com.banking.identity.repository.UserRepository;
import com.banking.identity.security.JwtTokenService;
import com.banking.identity.security.PasswordEncoder;
import com.banking.identity.web.dto.LoginRequest;
import com.banking.identity.web.dto.LoginResponse;
import com.banking.identity.web.dto.RegisterRequest;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AuthenticationService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_SECONDS = 1800; // 30 minutes

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final IdentityEventPublisher eventPublisher;

    public AuthenticationService(
            UserRepository userRepository,
            SessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            IdentityEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.eventPublisher = eventPublisher;
    }

    public User register(RegisterRequest request) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCustomerId(request.customerId());
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);

        User saved = userRepository.save(user);
        eventPublisher.publishUserRegistered(saved);
        return saved;
    }

    public LoginResponse login(LoginRequest request, String deviceId, String userAgent, String ipAddress) {
        User user = userRepository.findActiveByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        // Check if account is locked
        if (user.isLocked()) {
            throw new IllegalStateException("Account is locked. Please try again later.");
        }

        // Check if account is active
        if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Account is not active");
        }

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Reset failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getUsername(), user.getCustomerId());
        String refreshToken = jwtTokenService.generateRefreshToken(user.getId());

        // Create session
        Session session = new Session();
        session.setUserId(user.getId());
        session.setRefreshToken(refreshToken);
        session.setDeviceId(deviceId);
        session.setUserAgent(userAgent);
        session.setIpAddress(ipAddress);
        session.setStatus(SessionStatus.ACTIVE);
        session.setExpiresAt(Instant.now().plusSeconds(86400)); // 24 hours
        sessionRepository.save(session);

        eventPublisher.publishUserLoggedIn(user);

        return new LoginResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getCustomerId(),
                user.getEmail(),
                user.getEmailVerified()
        );
    }

    public void logout(String refreshToken) {
        sessionRepository.findByRefreshToken(refreshToken)
                .ifPresent(session -> {
                    session.setStatus(SessionStatus.LOGGED_OUT);
                    sessionRepository.save(session);
                    eventPublisher.publishUserLoggedOut(session.getUserId());
                });
    }

    public void logoutAll(UUID userId) {
        sessionRepository.revokeAllActiveSessions(userId, SessionStatus.LOGGED_OUT);
        eventPublisher.publishUserLoggedOut(userId);
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plusSeconds(LOCK_DURATION_SECONDS));
            user.setStatus(UserStatus.LOCKED);
            eventPublisher.publishUserLocked(user);
        }

        userRepository.save(user);
    }
}

