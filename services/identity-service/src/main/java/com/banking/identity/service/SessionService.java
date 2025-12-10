package com.banking.identity.service;

import com.banking.identity.domain.Session;
import com.banking.identity.domain.SessionStatus;
import com.banking.identity.repository.SessionRepository;
import com.banking.identity.security.JwtTokenService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final JwtTokenService jwtTokenService;

    public SessionService(SessionRepository sessionRepository, JwtTokenService jwtTokenService) {
        this.sessionRepository = sessionRepository;
        this.jwtTokenService = jwtTokenService;
    }

    public Session validateRefreshToken(String refreshToken) {
        // Validate JWT token
        jwtTokenService.validateToken(refreshToken);
        if (!jwtTokenService.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token type");
        }

        // Find session
        Session session = sessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        // Check if session is active and not expired
        if (!session.isActive()) {
            throw new IllegalStateException("Session is not active");
        }

        // Update last used timestamp
        session.setLastUsedAt(Instant.now());
        return sessionRepository.save(session);
    }

    public String refreshAccessToken(String refreshToken) {
        Session session = validateRefreshToken(refreshToken);
        UUID userId = jwtTokenService.getUserIdFromToken(refreshToken);
        
        // Get user info from session (in real implementation, fetch from user repository)
        // For now, we'll use the userId from the token
        return jwtTokenService.generateAccessToken(userId, "user", UUID.randomUUID());
    }

    public List<Session> getUserSessions(UUID userId) {
        return sessionRepository.findByUserIdAndStatus(userId, SessionStatus.ACTIVE);
    }

    public void revokeSession(UUID sessionId) {
        sessionRepository.findById(sessionId)
                .ifPresent(session -> {
                    session.setStatus(SessionStatus.REVOKED);
                    sessionRepository.save(session);
                });
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupExpiredSessions() {
        sessionRepository.deleteExpiredSessions();
    }
}

