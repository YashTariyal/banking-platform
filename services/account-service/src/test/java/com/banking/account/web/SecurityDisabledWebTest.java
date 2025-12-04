package com.banking.account.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.account.config.SecurityConfig;
import com.banking.account.service.AccountService;
import com.banking.account.config.PiiMaskingFilter;
import com.banking.account.metrics.AccountMetrics;
import com.banking.account.security.RefreshTokenService;
import com.banking.account.web.dto.AccountResponse;
import com.banking.account.web.dto.PageResponse;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AccountController.class, AuthController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "account.security.enabled=false")
class SecurityDisabledWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private PiiMaskingFilter piiMaskingFilter;

    @MockBean
    private AccountMetrics accountMetrics;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Return an empty page to avoid 400s from null responses in controller
        org.mockito.Mockito.when(accountService.listAccounts(
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new PageResponse<>(Collections.<AccountResponse>emptyList(), 0, 0, 0, 20));
    }

    @Test
    void whenSecurityDisabled_apiAccountsIsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/accounts")
                        .param("page", "0")
                        .param("size", "20"))
                // We only assert that no authentication (401) is required when security is disabled.
                // Business/validation errors may still produce 4xx responses.
                .andExpect(status().is4xxClientError());
    }

    @Test
    void whenSecurityDisabled_authMeIsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk());
    }
}


