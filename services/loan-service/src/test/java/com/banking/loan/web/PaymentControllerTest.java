package com.banking.loan.web;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.loan.domain.LoanPayment;
import com.banking.loan.domain.PaymentScheduleItem;
import com.banking.loan.domain.PaymentStatus;
import com.banking.loan.domain.PaymentType;
import com.banking.loan.service.LoanService;
import com.banking.loan.service.PaymentService;
import com.banking.loan.web.dto.LoanPaymentResponse;
import com.banking.loan.web.dto.PaymentScheduleItemResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanService loanService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private LoanMapper mapper;

    private PaymentScheduleItem scheduleItem;
    private PaymentScheduleItemResponse scheduleResponse;

    private LoanPayment payment;
    private LoanPaymentResponse paymentResponse;

    @BeforeEach
    void setup() {
        scheduleItem = new PaymentScheduleItem();
        scheduleItem.setId(UUID.randomUUID());
        scheduleItem.setInstallmentNumber(1);
        scheduleItem.setDueDate(LocalDate.of(2024, 2, 1));
        scheduleItem.setPrincipalDue(new BigDecimal("100.00"));
        scheduleItem.setInterestDue(new BigDecimal("10.00"));
        scheduleItem.setPrincipalPaid(BigDecimal.ZERO);
        scheduleItem.setInterestPaid(BigDecimal.ZERO);
        scheduleItem.setStatus(PaymentStatus.DUE);

        scheduleResponse = new PaymentScheduleItemResponse();
        scheduleResponse.setId(scheduleItem.getId());
        scheduleResponse.setInstallmentNumber(1);
        scheduleResponse.setDueDate(scheduleItem.getDueDate());
        scheduleResponse.setPrincipalDue(scheduleItem.getPrincipalDue());
        scheduleResponse.setInterestDue(scheduleItem.getInterestDue());
        scheduleResponse.setPrincipalPaid(scheduleItem.getPrincipalPaid());
        scheduleResponse.setInterestPaid(scheduleItem.getInterestPaid());
        scheduleResponse.setStatus(scheduleItem.getStatus());

        payment = new LoanPayment();
        payment.setId(UUID.randomUUID());
        payment.setLoanApplicationId(UUID.randomUUID());
        payment.setPaymentType(PaymentType.REGULAR);
        payment.setAmount(new BigDecimal("50.00"));
        payment.setPrincipalComponent(new BigDecimal("40.00"));
        payment.setInterestComponent(new BigDecimal("10.00"));
        payment.setPaymentDate(LocalDate.of(2024, 1, 15));

        paymentResponse = new LoanPaymentResponse();
        paymentResponse.setId(payment.getId());
        paymentResponse.setLoanApplicationId(payment.getLoanApplicationId());
        paymentResponse.setPaymentType(payment.getPaymentType());
        paymentResponse.setAmount(payment.getAmount());
        paymentResponse.setPrincipalComponent(payment.getPrincipalComponent());
        paymentResponse.setInterestComponent(payment.getInterestComponent());
        paymentResponse.setPaymentDate(payment.getPaymentDate());
    }

    @Test
    void schedule_returnsItems() throws Exception {
        UUID loanId = UUID.randomUUID();
        when(loanService.getSchedule(loanId)).thenReturn(List.of(scheduleItem));
        when(mapper.toResponse(scheduleItem)).thenReturn(scheduleResponse);

        mockMvc.perform(get("/api/loans/{id}/schedule", loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].installmentNumber", is(1)));
    }

    @Test
    void pay_createsPayment() throws Exception {
        UUID loanId = UUID.randomUUID();
        when(paymentService.applyPayment(eq(loanId), any(), eq(PaymentType.REGULAR), any())).thenReturn(payment);
        when(mapper.toResponse(payment)).thenReturn(paymentResponse);

        mockMvc.perform(post("/api/loans/{id}/payments", loanId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 50.00,
                                  "paymentType": "REGULAR",
                                  "paymentDate": "2024-01-15"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(50.00)));
    }
}

