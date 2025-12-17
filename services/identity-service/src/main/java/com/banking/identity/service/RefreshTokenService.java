package com.banking.identity.service;

import com.banking.identity.domain.Session;
import com.banking.identity.domain.SessionStatus;
import com.banking.identity.domain.User;
import com.banking.identity.repository.SessionRepository;
import com.banking.identity.repository.UserRepository;
import com.banking.identity.security.JwtTokenService;
import com.banking.identity.web.dto.RefreshTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    public RefreshTokenService(
            SessionRepository sessionRepository,
            UserRepository userRepository,
            JwtTokenService jwtTokenService) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public RefreshTokenResponse rotateRefreshToken(String oldRefreshToken) {
        // Validate the refresh token
        if (!jwtTokenService.isRefreshToken(oldRefreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        UUID userId = jwtTokenService.getUserIdFromToken(oldRefreshToken);

        // Find the session with this refresh token
        Session session = sessionRepository.findByRefreshToken(oldRefreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        // Check if session is active
        if (session.getStatus() != SessionStatus.ACTIVE) {
            log.warn("Attempted to use revoked/expired refresh token for user {}", userId);
            // Potential token reuse attack - revoke all sessions
            sessionRepository.revokeAllActiveSessions(userId, SessionStatus.REVOKED);
            throw new IllegalStateException("Refresh token has been revoked. All sessions terminated for security.");
        }

        // Check if token is expired
        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setStatus(SessionStatus.EXPIRED);
            sessionRepository.save(session);
            throw new IllegalArgumentException("Refresh token has expired");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Generate new tokens
        String newAccessToken = jwtTokenService.generateAccessToken(user.getId(), user.getUsername(), user.getCustomerId());
        String newRefreshToken = jwtTokenService.generateRefreshToken(user.getId());

        // Invalidate old refresh token and update with new one (rotation)
        session.setRefreshToken(newRefreshToken);
        session.setExpiresAt(Instant.now().plusSeconds(86400)); // 24 hours
        session.setRefreshedAt(Instant.now());
        sessionRepository.save(session);

        log.info("Refresh token rotated for user {}", userId);

        return new RefreshTokenResponse(newAccessToken, newRefreshToken);
    }
}
