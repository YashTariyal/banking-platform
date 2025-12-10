package com.banking.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.identity.domain.Session;
import com.banking.identity.domain.SessionStatus;
import com.banking.identity.repository.SessionRepository;
import com.banking.identity.security.JwtTokenService;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(sessionRepository, jwtTokenService);
    }

    @Test
    void validateRefreshToken_WithValidToken_ReturnsSession() {
        // Given
        String refreshToken = "validRefreshToken";
        UUID userId = UUID.randomUUID();
        Session session = createSession();
        session.setRefreshToken(refreshToken);

        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenService.validateToken(refreshToken)).thenReturn(claims);
        when(jwtTokenService.isRefreshToken(refreshToken)).thenReturn(true);
        when(sessionRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Session result = sessionService.validateRefreshToken(refreshToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRefreshToken()).isEqualTo(refreshToken);
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void validateRefreshToken_WithInvalidToken_ThrowsException() {
        // Given
        String invalidToken = "invalidToken";

        when(jwtTokenService.validateToken(invalidToken)).thenThrow(new IllegalArgumentException("Invalid token"));

        // When/Then
        assertThatThrownBy(() -> sessionService.validateRefreshToken(invalidToken))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateRefreshToken_WithInactiveSession_ThrowsException() {
        // Given
        String refreshToken = "refreshToken";
        Session session = createSession();
        session.setRefreshToken(refreshToken);
        session.setStatus(SessionStatus.LOGGED_OUT);

        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenService.validateToken(refreshToken)).thenReturn(claims);
        when(jwtTokenService.isRefreshToken(refreshToken)).thenReturn(true);
        when(sessionRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(session));

        // When/Then
        assertThatThrownBy(() -> sessionService.validateRefreshToken(refreshToken))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void refreshAccessToken_GeneratesNewAccessToken() {
        // Given
        String refreshToken = "refreshToken";
        UUID userId = UUID.randomUUID();
        Session session = createSession();
        session.setRefreshToken(refreshToken);

        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenService.validateToken(refreshToken)).thenReturn(claims);
        when(jwtTokenService.isRefreshToken(refreshToken)).thenReturn(true);
        when(sessionRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(session));
        when(jwtTokenService.getUserIdFromToken(refreshToken)).thenReturn(userId);
        when(jwtTokenService.generateAccessToken(any(), any(), any())).thenReturn("newAccessToken");
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        String newToken = sessionService.refreshAccessToken(refreshToken);

        // Then
        assertThat(newToken).isEqualTo("newAccessToken");
    }

    @Test
    void getUserSessions_ReturnsActiveSessions() {
        // Given
        UUID userId = UUID.randomUUID();
        List<Session> sessions = List.of(createSession(), createSession());

        when(sessionRepository.findByUserIdAndStatus(userId, SessionStatus.ACTIVE)).thenReturn(sessions);

        // When
        List<Session> result = sessionService.getUserSessions(userId);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void revokeSession_SetsStatusToRevoked() {
        // Given
        UUID sessionId = UUID.randomUUID();
        Session session = createSession();
        session.setId(sessionId);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        sessionService.revokeSession(sessionId);

        // Then
        verify(sessionRepository).save(any(Session.class));
    }

    private Session createSession() {
        Session session = new Session();
        session.setId(UUID.randomUUID());
        session.setUserId(UUID.randomUUID());
        session.setRefreshToken("refreshToken");
        session.setStatus(SessionStatus.ACTIVE);
        session.setExpiresAt(Instant.now().plusSeconds(86400));
        session.setLastUsedAt(Instant.now());
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        return session;
    }
}

