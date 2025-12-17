package com.banking.identity.service;

import com.banking.identity.domain.PasswordReset;
import com.banking.identity.domain.User;
import com.banking.identity.messaging.IdentityEventPublisher;
import com.banking.identity.repository.PasswordResetRepository;
import com.banking.identity.repository.UserRepository;
import com.banking.identity.security.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetRepository passwordResetRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityEventPublisher eventPublisher;
    private final long tokenValiditySeconds;

    public PasswordResetService(
            PasswordResetRepository passwordResetRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            IdentityEventPublisher eventPublisher,
            @Value("${identity.password-reset.token-validity-seconds:3600}") long tokenValiditySeconds) {
        this.passwordResetRepository = passwordResetRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.tokenValiditySeconds = tokenValiditySeconds;
    }

    @Transactional
    public String initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElse(null);

        // Always return success to prevent email enumeration
        if (user == null) {
            log.info("Password reset requested for non-existent email");
            return null;
        }

        // Invalidate any existing tokens
        passwordResetRepository.invalidateExistingTokens(user.getId());

        // Generate secure token
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Create password reset record
        PasswordReset reset = new PasswordReset();
        reset.setUserId(user.getId());
        reset.setToken(token);
        reset.setEmail(email);
        reset.setExpiresAt(Instant.now().plusSeconds(tokenValiditySeconds));
        passwordResetRepository.save(reset);

        // Publish event for notification service to send email
        eventPublisher.publishPasswordResetRequested(user, token);

        log.info("Password reset initiated for user {}", user.getId());
        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordReset reset = passwordResetRepository.findValidToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        User user = userRepository.findById(reset.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        // Mark token as used
        reset.setUsedAt(Instant.now());
        passwordResetRepository.save(reset);

        // Publish event
        eventPublisher.publishPasswordChanged(user);

        log.info("Password reset completed for user {}", user.getId());
    }

    public boolean validateToken(String token) {
        return passwordResetRepository.findValidToken(token).isPresent();
    }
}
