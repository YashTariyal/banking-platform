package com.banking.account.web;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountTransactionType;
import com.banking.account.domain.AccountType;
import com.banking.account.service.AccountService;
import com.banking.account.web.dto.AccountResponse;
import com.banking.account.web.dto.AccountTransactionRequest;
import com.banking.account.web.dto.BalanceResponse;
import com.banking.account.web.dto.CreateAccountRequest;
import com.banking.account.web.dto.PageResponse;
import com.banking.account.web.dto.TransactionHistoryResponse;
import com.banking.account.web.dto.UpdateAccountRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AccountController.class)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @Test
    void createAccountReturns201() throws Exception {
        UUID accountId = UUID.randomUUID();
        AccountResponse response = sampleResponse(accountId);
        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(response);

        CreateAccountRequest request = new CreateAccountRequest(UUID.randomUUID(), AccountType.CHECKING, "USD", BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", equalTo(accountId.toString())));
    }

    @Test
    void listAccountsSupportsPagination() throws Exception {
        PageResponse<AccountResponse> page = new PageResponse<>(
                List.of(sampleResponse(UUID.randomUUID())),
                1,
                1,
                0,
                20
        );
        when(accountService.listAccounts(any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/accounts")
                        .param("customerId", UUID.randomUUID().toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", equalTo(1)))
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void applyTransactionInvokesService() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.applyTransaction(any(), any(AccountTransactionRequest.class))).thenReturn(sampleResponse(accountId));
        AccountTransactionRequest request = new AccountTransactionRequest(UUID.randomUUID(), AccountTransactionType.CREDIT, BigDecimal.valueOf(50), "bonus");

        mockMvc.perform(post("/api/accounts/{id}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(accountId.toString())));
    }

    @Test
    void getBalanceReturnsBalanceResponse() throws Exception {
        UUID accountId = UUID.randomUUID();
        BalanceResponse balanceResponse = new BalanceResponse(
                accountId,
                "ACC-123456789012",
                BigDecimal.valueOf(500.50),
                "USD"
        );
        when(accountService.getBalance(accountId)).thenReturn(balanceResponse);

        mockMvc.perform(get("/api/accounts/{id}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", equalTo(accountId.toString())))
                .andExpect(jsonPath("$.accountNumber", equalTo("ACC-123456789012")))
                .andExpect(jsonPath("$.balance", equalTo(500.50)))
                .andExpect(jsonPath("$.currency", equalTo("USD")));
    }

    @Test
    void getTransactionHistoryReturnsPagedTransactions() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID referenceId1 = UUID.randomUUID();
        UUID referenceId2 = UUID.randomUUID();
        PageResponse<TransactionHistoryResponse> page = new PageResponse<>(
                List.of(
                        new TransactionHistoryResponse(
                                referenceId1,
                                AccountTransactionType.CREDIT,
                                BigDecimal.valueOf(100.00),
                                BigDecimal.valueOf(200.00),
                                Instant.now()
                        ),
                        new TransactionHistoryResponse(
                                referenceId2,
                                AccountTransactionType.DEBIT,
                                BigDecimal.valueOf(50.00),
                                BigDecimal.valueOf(150.00),
                                Instant.now()
                        )
                ),
                2,
                2,
                0,
                20
        );
        when(accountService.getTransactionHistory(any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/accounts/{id}/transactions", accountId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", equalTo(2)))
                .andExpect(jsonPath("$.totalElements", equalTo(2)))
                .andExpect(jsonPath("$.items[0].type", equalTo("CREDIT")))
                .andExpect(jsonPath("$.items[1].type", equalTo("DEBIT")));
    }

    @Test
    void updateAccountInvokesService() throws Exception {
        UUID accountId = UUID.randomUUID();
        AccountResponse response = sampleResponse(accountId);
        when(accountService.updateAccount(any(), any(UpdateAccountRequest.class))).thenReturn(response);
        UpdateAccountRequest request = new UpdateAccountRequest(AccountType.SAVINGS, "EUR");

        mockMvc.perform(put("/api/accounts/{id}", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(accountId.toString())));
    }

    private AccountResponse sampleResponse(UUID accountId) {
        return new AccountResponse(
                accountId,
                "ACC-123456789012",
                UUID.randomUUID(),
                AccountType.CHECKING,
                AccountStatus.ACTIVE,
                "USD",
                BigDecimal.valueOf(100),
                Instant.now(),
                Instant.now()
        );
    }
}

