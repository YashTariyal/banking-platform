package com.banking.transaction.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.transaction.domain.Transaction;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.domain.TransactionType;
import com.banking.transaction.service.TransactionService;
import com.banking.transaction.web.dto.CreateTransactionRequest;
import com.banking.transaction.web.dto.FailTransactionRequest;
import com.banking.transaction.web.dto.PageResponse;
import com.banking.transaction.web.dto.ReverseTransactionRequest;
import com.banking.transaction.web.dto.TransactionResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private TransactionMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID transactionId;
    private UUID customerId;
    private String referenceId;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        referenceId = "REF-123";
    }

    @Test
    void createTransaction_validRequest_returnsCreated() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                referenceId,
                TransactionType.DEPOSIT,
                customerId,
                null,
                null,
                new BigDecimal("100"),
                "USD",
                "Test deposit"
        );

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setReferenceId(referenceId);
        transaction.setTransactionType(TransactionType.DEPOSIT);

        TransactionResponse response = new TransactionResponse(
                transactionId,
                referenceId,
                TransactionType.DEPOSIT,
                TransactionStatus.PENDING,
                customerId,
                null,
                null,
                new BigDecimal("100"),
                "USD",
                "Test deposit",
                null,
                Instant.now(),
                null,
                Instant.now(),
                Instant.now()
        );

        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(transaction);
        when(mapper.toResponse(transaction)).thenReturn(response);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getTransaction_exists_returnsOk() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setReferenceId(referenceId);

        TransactionResponse response = new TransactionResponse(
                transactionId,
                referenceId,
                TransactionType.DEPOSIT,
                TransactionStatus.PENDING,
                customerId,
                null,
                null,
                new BigDecimal("100"),
                "USD",
                null,
                null,
                Instant.now(),
                null,
                Instant.now(),
                Instant.now()
        );

        when(transactionService.getTransaction(transactionId)).thenReturn(transaction);
        when(mapper.toResponse(transaction)).thenReturn(response);

        mockMvc.perform(get("/api/transactions/{id}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId.toString()));
    }

    @Test
    void processTransaction_validRequest_returnsOk() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setStatus(TransactionStatus.PROCESSING);

        TransactionResponse response = new TransactionResponse(
                transactionId,
                referenceId,
                TransactionType.DEPOSIT,
                TransactionStatus.PROCESSING,
                customerId,
                null,
                null,
                new BigDecimal("100"),
                "USD",
                null,
                null,
                Instant.now(),
                null,
                Instant.now(),
                Instant.now()
        );

        when(transactionService.processTransaction(transactionId)).thenReturn(transaction);
        when(mapper.toResponse(transaction)).thenReturn(response);

        mockMvc.perform(put("/api/transactions/{id}/process", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void failTransaction_validRequest_returnsOk() throws Exception {
        FailTransactionRequest request = new FailTransactionRequest("Insufficient funds");

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason("Insufficient funds");

        TransactionResponse response = new TransactionResponse(
                transactionId,
                referenceId,
                TransactionType.DEPOSIT,
                TransactionStatus.FAILED,
                customerId,
                null,
                null,
                new BigDecimal("100"),
                "USD",
                null,
                "Insufficient funds",
                Instant.now(),
                null,
                Instant.now(),
                Instant.now()
        );

        when(transactionService.failTransaction(transactionId, request.failureReason()))
                .thenReturn(transaction);
        when(mapper.toResponse(transaction)).thenReturn(response);

        mockMvc.perform(put("/api/transactions/{id}/fail", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Insufficient funds"));
    }
}

