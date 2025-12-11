package com.banking.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.banking.loan.domain.LoanApplication;
import com.banking.loan.domain.LoanPayment;
import com.banking.loan.domain.LoanStatus;
import com.banking.loan.domain.PaymentScheduleItem;
import com.banking.loan.domain.PaymentType;
import com.banking.loan.repository.LoanApplicationRepository;
import com.banking.loan.repository.LoanPaymentRepository;
import com.banking.loan.repository.PaymentScheduleItemRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private LoanApplicationRepository loanRepository;
    @Mock
    private PaymentScheduleItemRepository scheduleRepository;
    @Mock
    private LoanPaymentRepository paymentRepository;

    private Clock clock;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        paymentService = new PaymentService(loanRepository, scheduleRepository, paymentRepository, clock);
    }

    @Test
    void applyPayment_allocatesAndSaves() {
        UUID loanId = UUID.randomUUID();
        LoanApplication loan = new LoanApplication();
        loan.setId(loanId);
        loan.setStatus(LoanStatus.ACTIVE);

        PaymentScheduleItem item = new PaymentScheduleItem();
        item.setId(UUID.randomUUID());
        item.setLoanApplicationId(loanId);
        item.setInstallmentNumber(1);
        item.setDueDate(LocalDate.of(2024, 2, 1));
        item.setPrincipalDue(new BigDecimal("100.00"));
        item.setInterestDue(new BigDecimal("10.00"));

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(scheduleRepository.findByLoanApplicationIdOrderByInstallmentNumber(loanId)).thenReturn(List.of(item));
        when(paymentRepository.save(any(LoanPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanPayment payment = paymentService.applyPayment(
                loanId,
                new BigDecimal("50.00"),
                PaymentType.REGULAR,
                LocalDate.of(2024, 1, 15)
        );

        assertThat(payment.getAmount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(item.getInterestPaid().add(item.getPrincipalPaid())).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void applyPayment_missingSchedule_throws() {
        UUID loanId = UUID.randomUUID();
        LoanApplication loan = new LoanApplication();
        loan.setId(loanId);
        loan.setStatus(LoanStatus.ACTIVE);
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(scheduleRepository.findByLoanApplicationIdOrderByInstallmentNumber(loanId)).thenReturn(List.of());

        assertThatThrownBy(() -> paymentService.applyPayment(loanId, BigDecimal.TEN, PaymentType.REGULAR, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}

