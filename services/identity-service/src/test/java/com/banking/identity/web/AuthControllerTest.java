package com.banking.identity.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.identity.domain.User;
import com.banking.identity.domain.UserStatus;
import com.banking.identity.service.AuthenticationService;
import com.banking.identity.service.SessionService;
import com.banking.identity.web.dto.LoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private SessionService sessionService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void register_CreatesNewUser() throws Exception {
        // Given
        com.banking.identity.web.dto.RegisterRequest request =
                new com.banking.identity.web.dto.RegisterRequest(
                        "testuser", "test@example.com", "password123", UUID.randomUUID()
                );

        User user = createUser();
        when(authenticationService.register(any())).thenReturn(user);

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(authenticationService).register(any());
    }

    @Test
    void login_ReturnsTokens() throws Exception {
        // Given
        com.banking.identity.web.dto.LoginRequest request =
                new com.banking.identity.web.dto.LoginRequest("testuser", "password123");

        LoginResponse response = new LoginResponse(
                "accessToken",
                "refreshToken",
                UUID.randomUUID(),
                "testuser",
                UUID.randomUUID(),
                "test@example.com",
                false
        );

        when(authenticationService.login(any(), any(), any(), any())).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.refreshToken").value("refreshToken"));

        verify(authenticationService).login(any(), any(), any(), any());
    }

    @Test
    void refresh_ReturnsNewAccessToken() throws Exception {
        // Given
        com.banking.identity.web.dto.RefreshTokenRequest request =
                new com.banking.identity.web.dto.RefreshTokenRequest("refreshToken");

        when(sessionService.refreshAccessToken("refreshToken")).thenReturn("newAccessToken");

        // When/Then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccessToken"));

        verify(sessionService).refreshAccessToken("refreshToken");
    }

    private User createUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setCustomerId(UUID.randomUUID());
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        return user;
    }
}

