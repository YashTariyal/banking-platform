package com.banking.ledger.web;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.ledger.domain.LedgerAccount;
import com.banking.ledger.domain.LedgerAccountStatus;
import com.banking.ledger.domain.LedgerAccountType;
import com.banking.ledger.service.LedgerAccountService;
import com.banking.ledger.web.dto.LedgerAccountResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LedgerAccountController.class)
class LedgerAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LedgerAccountService ledgerAccountService;

    @MockBean
    private LedgerMapper mapper;

    private LedgerAccount account;

    @BeforeEach
    void setup() {
        account = new LedgerAccount();
        account.setId(UUID.randomUUID());
        account.setName("Cash");
        account.setType(LedgerAccountType.ASSET);
        account.setStatus(LedgerAccountStatus.ACTIVE);
        account.setCurrency("USD");
        account.setBalance(new BigDecimal("0.00"));
        account.setCreatedAt(Instant.now());
        account.setUpdatedAt(Instant.now());
    }

    @Test
    void create_returnsCreatedAccount() throws Exception {
        LedgerAccountResponse response = new LedgerAccountResponse();
        response.setId(account.getId());
        response.setName(account.getName());
        response.setType(account.getType());
        response.setStatus(account.getStatus());
        response.setCurrency(account.getCurrency());
        response.setBalance(account.getBalance());

        when(ledgerAccountService.create(any(LedgerAccount.class))).thenReturn(account);
        when(mapper.toResponse(account)).thenReturn(response);

        mockMvc.perform(post("/api/ledger/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Cash",
                                  "type": "ASSET",
                                  "currency": "USD"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Cash")))
                .andExpect(jsonPath("$.type", is("ASSET")));
    }

    @Test
    void list_returnsPagedAccounts() throws Exception {
        LedgerAccountResponse response = new LedgerAccountResponse();
        response.setId(account.getId());
        response.setName(account.getName());
        response.setType(account.getType());
        response.setStatus(account.getStatus());
        response.setCurrency(account.getCurrency());
        response.setBalance(account.getBalance());

        when(ledgerAccountService.list(PageRequest.of(0, 20))).thenReturn(new PageImpl<>(List.of(account)));
        when(mapper.toPageResponse(any(), any())).thenCallRealMethod();
        when(mapper.toResponse(account)).thenReturn(response);

        mockMvc.perform(get("/api/ledger/accounts?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name", is("Cash")));
    }

    @Test
    void updateStatus_updatesAccount() throws Exception {
        LedgerAccountResponse response = new LedgerAccountResponse();
        response.setId(account.getId());
        response.setName(account.getName());
        response.setType(account.getType());
        response.setStatus(LedgerAccountStatus.SUSPENDED);
        response.setCurrency(account.getCurrency());
        response.setBalance(account.getBalance());

        when(ledgerAccountService.updateStatus(account.getId(), LedgerAccountStatus.SUSPENDED)).thenReturn(account);
        when(mapper.toResponse(account)).thenReturn(response);

        mockMvc.perform(put("/api/ledger/accounts/{id}/status", account.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "SUSPENDED" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUSPENDED")));
    }
}

