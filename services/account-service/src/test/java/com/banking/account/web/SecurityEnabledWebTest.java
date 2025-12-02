package com.banking.account.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.banking.account.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class})
@TestPropertySource(properties = {
        "account.security.enabled=true"
})
class SecurityEnabledWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenSecurityEnabled_requestsWithoutTokenReturn401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenSecurityEnabled_requestsWithValidJwtSucceed() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("user-123")
                                .claim("scope", "accounts.read accounts.write")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("user-123"))
                .andExpect(jsonPath("$.scopes[0]").value("accounts.read"));
    }
}


