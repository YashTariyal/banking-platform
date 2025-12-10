package com.banking.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private IdentityEventPublisher eventPublisher;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                userRepository,
                sessionRepository,
                passwordEncoder,
                jwtTokenService,
                eventPublisher
        );
    }

    @Test
    void register_CreatesNewUser() {
        // Given
        UUID customerId = UUID.randomUUID();
        RegisterRequest request = new RegisterRequest(
                "testuser",
                "test@example.com",
                "password123",
                customerId
        );

        User savedUser = createUser();
        savedUser.setUsername("testuser");
        savedUser.setEmail("test@example.com");
        savedUser.setCustomerId(customerId);

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        // When
        User result = authenticationService.register(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishUserRegistered(any(User.class));
    }

    @Test
    void register_WithDuplicateUsername_ThrowsException() {
        // Given
        RegisterRequest request = new RegisterRequest(
                "testuser", "test@example.com", "password123", UUID.randomUUID()
        );

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void login_WithValidCredentials_ReturnsTokens() {
        // Given
        User user = createUser();
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(0);

        LoginRequest request = new LoginRequest("testuser", "password123");

        when(userRepository.findActiveByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
        when(jwtTokenService.generateAccessToken(any(), any(), any())).thenReturn("accessToken");
        when(jwtTokenService.generateRefreshToken(any())).thenReturn("refreshToken");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        LoginResponse response = authenticationService.login(request, "device123", "user-agent", "127.0.0.1");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
        assertThat(response.userId()).isEqualTo(user.getId());
        verify(sessionRepository).save(any(Session.class));
        verify(eventPublisher).publishUserLoggedIn(any(User.class));
    }

    @Test
    void login_WithInvalidPassword_LocksAccountAfterMaxAttempts() {
        // Given
        User user = createUser();
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(4); // One attempt away from lockout

        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        when(userRepository.findActiveByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", user.getPasswordHash())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When/Then
        assertThatThrownBy(() -> authenticationService.login(request, "device123", "user-agent", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishUserLocked(any(User.class));
    }

    @Test
    void login_WithLockedAccount_ThrowsException() {
        // Given
        User user = createUser();
        user.setStatus(UserStatus.LOCKED);
        user.setLockedUntil(Instant.now().plusSeconds(1800));

        LoginRequest request = new LoginRequest("testuser", "password123");

        when(userRepository.findActiveByUsername("testuser")).thenReturn(Optional.of(user));

        // When/Then
        assertThatThrownBy(() -> authenticationService.login(request, "device123", "user-agent", "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void logout_InvalidatesSession() {
        // Given
        UUID userId = UUID.randomUUID();
        Session session = new Session();
        session.setUserId(userId);
        session.setRefreshToken("refreshToken");
        session.setStatus(SessionStatus.ACTIVE);

        when(sessionRepository.findByRefreshToken("refreshToken")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        authenticationService.logout("refreshToken");

        // Then
        verify(sessionRepository).save(any(Session.class));
        verify(eventPublisher).publishUserLoggedOut(userId);
    }

    private User createUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");
        user.setCustomerId(UUID.randomUUID());
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }
}

