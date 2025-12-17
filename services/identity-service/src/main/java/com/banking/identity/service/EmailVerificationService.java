package com.banking.identity.service;

import com.banking.identity.domain.EmailVerificationToken;
import com.banking.identity.domain.User;
import com.banking.identity.domain.UserStatus;
import com.banking.identity.messaging.IdentityEventPublisher;
import com.banking.identity.repository.EmailVerificationTokenRepository;
import com.banking.identity.repository.UserRepository;
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
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final IdentityEventPublisher eventPublisher;
    private final long tokenValiditySeconds;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            IdentityEventPublisher eventPublisher,
            @Value("${identity.email-verification.token-validity-seconds:86400}") long tokenValiditySeconds) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.tokenValiditySeconds = tokenValiditySeconds;
    }

    @Transactional
    public String createVerificationToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Generate secure token
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Create verification token
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUserId(userId);
        verificationToken.setToken(token);
        verificationToken.setEmail(user.getEmail());
        verificationToken.setExpiresAt(Instant.now().plusSeconds(tokenValiditySeconds));
        tokenRepository.save(verificationToken);

        // Publish event for notification service
        eventPublisher.publishEmailVerificationRequested(user, token);

        log.info("Email verification token created for user {}", userId);
        return token;
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findValidToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification token"));

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update user
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(Instant.now());
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);

        // Mark token as used
        verificationToken.setUsedAt(Instant.now());
        tokenRepository.save(verificationToken);

        // Publish event
        eventPublisher.publishEmailVerified(user);

        log.info("Email verified for user {}", user.getId());
    }

    @Transactional
    public String resendVerificationEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getEmailVerified()) {
            throw new IllegalStateException("Email is already verified");
        }

        return createVerificationToken(userId);
    }
}
