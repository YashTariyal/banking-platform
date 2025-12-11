package com.banking.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.banking.loan.domain.LoanApplication;
import com.banking.loan.domain.LoanStatus;
import com.banking.loan.domain.PaymentScheduleItem;
import com.banking.loan.repository.LoanApplicationRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;
    @Mock
    private PaymentScheduleItemRepository scheduleRepository;

    private Clock clock;

    @InjectMocks
    private LoanService loanService;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        loanService = new LoanService(loanApplicationRepository, scheduleRepository, clock);
    }

    @Test
    void approve_generatesSchedule_andActivatesLoan() {
        LoanApplication loan = baseLoan();
        when(loanApplicationRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(loanApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanApplication approved = loanService.approve(loan.getId(), LocalDate.of(2024, 2, 1));

        assertThat(approved.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(approved.getApprovedAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
    }

    @Test
    void reject_setsRejectedStatus() {
        LoanApplication loan = baseLoan();
        when(loanApplicationRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(loanApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanApplication rejected = loanService.reject(loan.getId());

        assertThat(rejected.getStatus()).isEqualTo(LoanStatus.REJECTED);
    }

    @Test
    void list_filtersByCustomerId() {
        LoanApplication loan = baseLoan();
        Page<LoanApplication> page = new PageImpl<>(List.of(loan));
        when(loanApplicationRepository.findByCustomerId(loan.getCustomerId(), PageRequest.of(0, 10))).thenReturn(page);

        Page<LoanApplication> result = loanService.list(PageRequest.of(0, 10), loan.getCustomerId());

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void approve_nonPending_throws() {
        LoanApplication loan = baseLoan();
        loan.setStatus(LoanStatus.APPROVED);
        when(loanApplicationRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.approve(loan.getId(), null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private LoanApplication baseLoan() {
        LoanApplication loan = new LoanApplication();
        loan.setId(UUID.randomUUID());
        loan.setCustomerId(UUID.randomUUID());
        loan.setPrincipal(new BigDecimal("1000.00"));
        loan.setAnnualInterestRate(new BigDecimal("12.0"));
        loan.setTermMonths(12);
        loan.setStatus(LoanStatus.PENDING);
        return loan;
    }
}

