package com.banking.payment.web;

import com.banking.payment.domain.Payment;
import com.banking.payment.domain.PaymentDirection;
import com.banking.payment.domain.PaymentRail;
import com.banking.payment.domain.PaymentStatus;
import com.banking.payment.service.PaymentService;
import com.banking.payment.web.dto.CreatePaymentRequest;
import com.banking.payment.web.dto.FailPaymentRequest;
import com.banking.payment.web.dto.ProcessPaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private PaymentMapper mapper;

    private ObjectMapper objectMapper;
    private UUID paymentId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private Payment payment;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        paymentId = UUID.randomUUID();
        fromAccountId = UUID.randomUUID();
        toAccountId = UUID.randomUUID();

        payment = new Payment();
        payment.setId(paymentId);
        payment.setReferenceId("REF-123");
        payment.setRail(PaymentRail.ACH);
        payment.setDirection(PaymentDirection.OUTBOUND);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setFromAccountId(fromAccountId);
        payment.setToAccountId(toAccountId);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("USD");
        payment.setDescription("Test payment");
        payment.setInitiatedAt(Instant.now());
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
    }

    @Test
    void createPayment_CreatesNewPayment() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                "REF-123",
                PaymentRail.ACH,
                PaymentDirection.OUTBOUND,
                fromAccountId,
                toAccountId,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                "USD",
                "Test payment"
        );

        when(paymentService.initiatePayment(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(payment);
        when(mapper.toResponse(any(Payment.class)))
                .thenReturn(new com.banking.payment.web.dto.PaymentResponse(
                        payment.getId(),
                        payment.getReferenceId(),
                        payment.getRail(),
                        payment.getDirection(),
                        payment.getStatus(),
                        payment.getFromAccountId(),
                        payment.getToAccountId(),
                        payment.getToExternalAccount(),
                        payment.getToExternalRouting(),
                        payment.getToExternalBankName(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getDescription(),
                        payment.getFailureReason(),
                        payment.getExternalReference(),
                        payment.getInitiatedAt(),
                        payment.getCompletedAt(),
                        payment.getCreatedAt(),
                        payment.getUpdatedAt()
                ));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.referenceId").value("REF-123"))
                .andExpect(jsonPath("$.rail").value("ACH"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getPayment_ReturnsPayment() throws Exception {
        when(paymentService.getPayment(paymentId)).thenReturn(payment);
        when(mapper.toResponse(any(Payment.class)))
                .thenReturn(new com.banking.payment.web.dto.PaymentResponse(
                        payment.getId(),
                        payment.getReferenceId(),
                        payment.getRail(),
                        payment.getDirection(),
                        payment.getStatus(),
                        payment.getFromAccountId(),
                        payment.getToAccountId(),
                        payment.getToExternalAccount(),
                        payment.getToExternalRouting(),
                        payment.getToExternalBankName(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getDescription(),
                        payment.getFailureReason(),
                        payment.getExternalReference(),
                        payment.getInitiatedAt(),
                        payment.getCompletedAt(),
                        payment.getCreatedAt(),
                        payment.getUpdatedAt()
                ));

        mockMvc.perform(get("/api/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()));
    }

    @Test
    void processPayment_ProcessesPayment() throws Exception {
        payment.setStatus(PaymentStatus.PROCESSING);
        ProcessPaymentRequest request = new ProcessPaymentRequest("EXT-REF-123");

        when(paymentService.processPayment(paymentId)).thenReturn(payment);
        when(paymentService.completePayment(eq(paymentId), any())).thenReturn(payment);
        when(mapper.toResponse(any(Payment.class)))
                .thenReturn(new com.banking.payment.web.dto.PaymentResponse(
                        payment.getId(),
                        payment.getReferenceId(),
                        payment.getRail(),
                        payment.getDirection(),
                        payment.getStatus(),
                        payment.getFromAccountId(),
                        payment.getToAccountId(),
                        payment.getToExternalAccount(),
                        payment.getToExternalRouting(),
                        payment.getToExternalBankName(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getDescription(),
                        payment.getFailureReason(),
                        payment.getExternalReference(),
                        payment.getInitiatedAt(),
                        payment.getCompletedAt(),
                        payment.getCreatedAt(),
                        payment.getUpdatedAt()
                ));

        mockMvc.perform(put("/api/payments/{id}/process", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void failPayment_FailsPayment() throws Exception {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason("Insufficient funds");
        FailPaymentRequest request = new FailPaymentRequest("Insufficient funds");

        when(paymentService.failPayment(paymentId, "Insufficient funds")).thenReturn(payment);
        when(mapper.toResponse(any(Payment.class)))
                .thenReturn(new com.banking.payment.web.dto.PaymentResponse(
                        payment.getId(),
                        payment.getReferenceId(),
                        payment.getRail(),
                        payment.getDirection(),
                        payment.getStatus(),
                        payment.getFromAccountId(),
                        payment.getToAccountId(),
                        payment.getToExternalAccount(),
                        payment.getToExternalRouting(),
                        payment.getToExternalBankName(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getDescription(),
                        payment.getFailureReason(),
                        payment.getExternalReference(),
                        payment.getInitiatedAt(),
                        payment.getCompletedAt(),
                        payment.getCreatedAt(),
                        payment.getUpdatedAt()
                ));

        mockMvc.perform(put("/api/payments/{id}/fail", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Insufficient funds"));
    }
}

