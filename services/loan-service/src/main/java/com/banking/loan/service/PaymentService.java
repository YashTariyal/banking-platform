package com.banking.loan.service;

import com.banking.loan.domain.LoanApplication;
import com.banking.loan.domain.LoanPayment;
import com.banking.loan.domain.PaymentScheduleItem;
import com.banking.loan.domain.PaymentStatus;
import com.banking.loan.domain.PaymentType;
import com.banking.loan.repository.LoanApplicationRepository;
import com.banking.loan.repository.LoanPaymentRepository;
import com.banking.loan.repository.PaymentScheduleItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

    private final LoanApplicationRepository loanRepository;
    private final PaymentScheduleItemRepository scheduleRepository;
    private final LoanPaymentRepository paymentRepository;
    private final Clock clock;

    public PaymentService(
            LoanApplicationRepository loanRepository,
            PaymentScheduleItemRepository scheduleRepository,
            LoanPaymentRepository paymentRepository,
            Clock clock
    ) {
        this.loanRepository = loanRepository;
        this.scheduleRepository = scheduleRepository;
        this.paymentRepository = paymentRepository;
        this.clock = clock;
    }

    @Transactional
    public LoanPayment applyPayment(UUID loanId, BigDecimal amount, PaymentType type, LocalDate paymentDate) {
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        if (loan.getStatus() == null || loan.getStatus().name().equals("REJECTED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan not active");
        }

        List<PaymentScheduleItem> schedule = scheduleRepository
                .findByLoanApplicationIdOrderByInstallmentNumber(loanId);
        if (schedule.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schedule not generated");
        }

        BigDecimal remaining = amount.setScale(2, RoundingMode.HALF_UP);
        for (PaymentScheduleItem item : schedule) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal interestRemaining = item.getInterestDue().subtract(item.getInterestPaid());
            BigDecimal appliedToInterest = interestRemaining.min(remaining);
            item.setInterestPaid(item.getInterestPaid().add(appliedToInterest));
            remaining = remaining.subtract(appliedToInterest);

            if (remaining.signum() > 0) {
                BigDecimal principalRemaining = item.getPrincipalDue().subtract(item.getPrincipalPaid());
                BigDecimal appliedToPrincipal = principalRemaining.min(remaining);
                item.setPrincipalPaid(item.getPrincipalPaid().add(appliedToPrincipal));
                remaining = remaining.subtract(appliedToPrincipal);
            }

            boolean fullyPaid = item.getPrincipalPaid().compareTo(item.getPrincipalDue()) >= 0
                    && item.getInterestPaid().compareTo(item.getInterestDue()) >= 0;
            if (fullyPaid) {
                item.setStatus(PaymentStatus.PAID);
            } else if (LocalDate.now(clock).isAfter(item.getDueDate())) {
                item.setStatus(PaymentStatus.LATE);
            }
        }
        scheduleRepository.saveAll(schedule);

        LoanPayment payment = new LoanPayment();
        payment.setLoanApplicationId(loanId);
        payment.setPaymentType(type);
        payment.setPaymentDate(paymentDate != null ? paymentDate : LocalDate.now(clock));
        payment.setAmount(amount);
        payment.setPrincipalComponent(amount.subtract(remaining)); // rough allocation recorded as total applied
        payment.setInterestComponent(BigDecimal.ZERO); // already tracked per schedule; placeholder
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public List<LoanPayment> listPayments(UUID loanId) {
        if (!loanRepository.existsById(loanId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found");
        }
        return paymentRepository.findByLoanApplicationIdOrderByPaymentDate(loanId);
    }
}

