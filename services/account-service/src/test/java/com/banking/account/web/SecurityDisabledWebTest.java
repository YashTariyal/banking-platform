package com.banking.account.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.account.config.SecurityConfig;
import com.banking.account.service.AccountService;
import com.banking.account.config.PiiMaskingFilter;
import com.banking.account.metrics.AccountMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AccountController.class, AuthController.class})
@AutoConfigureMockMvc(addFilters = true)
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

    @Test
    void whenSecurityDisabled_apiAccountsIsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk());
    }

    @Test
    void whenSecurityDisabled_authMeIsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk());
    }
}


