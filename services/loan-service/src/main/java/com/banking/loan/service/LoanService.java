package com.banking.loan.service;

import com.banking.loan.domain.LoanApplication;
import com.banking.loan.domain.LoanStatus;
import com.banking.loan.domain.PaymentScheduleItem;
import com.banking.loan.repository.LoanApplicationRepository;
import com.banking.loan.repository.PaymentScheduleItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoanService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final PaymentScheduleItemRepository scheduleRepository;
    private final Clock clock;

    public LoanService(
            LoanApplicationRepository loanApplicationRepository,
            PaymentScheduleItemRepository scheduleRepository,
            Clock clock
    ) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.scheduleRepository = scheduleRepository;
        this.clock = clock;
    }

    @Transactional
    public LoanApplication create(LoanApplication application) {
        application.setStatus(LoanStatus.PENDING);
        return loanApplicationRepository.save(application);
    }

    @Transactional
    public LoanApplication approve(UUID id, LocalDate firstDueDate) {
        LoanApplication application = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        if (application.getStatus() != LoanStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan not in pending status");
        }
        application.setStatus(LoanStatus.ACTIVE);
        application.setApprovedAt(Instant.now(clock));
        application.setDisbursedAt(Instant.now(clock));
        application.setFirstDueDate(firstDueDate != null ? firstDueDate : LocalDate.now(clock).plusMonths(1));
        loanApplicationRepository.save(application);

        List<PaymentScheduleItem> schedule = generateSchedule(application);
        scheduleRepository.saveAll(schedule);
        return application;
    }

    @Transactional
    public LoanApplication reject(UUID id) {
        LoanApplication application = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        if (application.getStatus() != LoanStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan not in pending status");
        }
        application.setStatus(LoanStatus.REJECTED);
        application.setRejectedAt(Instant.now(clock));
        return loanApplicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public LoanApplication get(UUID id) {
        return loanApplicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
    }

    @Transactional(readOnly = true)
    public Page<LoanApplication> list(Pageable pageable, UUID customerId) {
        if (customerId != null) {
            return loanApplicationRepository.findByCustomerId(customerId, pageable);
        }
        return loanApplicationRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<PaymentScheduleItem> getSchedule(UUID loanId) {
        ensureExists(loanId);
        return scheduleRepository.findByLoanApplicationIdOrderByInstallmentNumber(loanId);
    }

    private void ensureExists(UUID loanId) {
        if (!loanApplicationRepository.existsById(loanId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found");
        }
    }

    private List<PaymentScheduleItem> generateSchedule(LoanApplication application) {
        BigDecimal principal = application.getPrincipal();
        BigDecimal monthlyRate = application.getAnnualInterestRate()
                .divide(BigDecimal.valueOf(12 * 100.0), 10, RoundingMode.HALF_UP);
        int n = application.getTermMonths();

        BigDecimal numerator = monthlyRate.multiply(principal);
        BigDecimal denominator = BigDecimal.ONE.subtract(
                BigDecimal.ONE.add(monthlyRate).pow(-n, java.math.MathContext.DECIMAL64)
        );
        BigDecimal installment = denominator.signum() == 0
                ? principal.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP)
                : numerator.divide(denominator, 2, RoundingMode.HALF_UP);

        List<PaymentScheduleItem> items = new ArrayList<>();
        BigDecimal remainingPrincipal = principal;
        LocalDate dueDate = application.getFirstDueDate() != null
                ? application.getFirstDueDate()
                : LocalDate.now(clock).plusMonths(1);

        for (int i = 1; i <= n; i++) {
            BigDecimal interestPortion = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPortion = installment.subtract(interestPortion).setScale(2, RoundingMode.HALF_UP);
            if (i == n) {
                principalPortion = remainingPrincipal;
            }
            remainingPrincipal = remainingPrincipal.subtract(principalPortion);

            PaymentScheduleItem item = new PaymentScheduleItem();
            item.setId(UUID.randomUUID());
            item.setLoanApplicationId(application.getId());
            item.setInstallmentNumber(i);
            item.setDueDate(dueDate);
            item.setPrincipalDue(principalPortion);
            item.setInterestDue(interestPortion);
            items.add(item);
            dueDate = dueDate.plusMonths(1);
        }
        return items;
    }
}

