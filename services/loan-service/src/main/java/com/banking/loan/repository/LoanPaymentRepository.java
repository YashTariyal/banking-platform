package com.banking.loan.repository;

import com.banking.loan.domain.LoanPayment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanPaymentRepository extends JpaRepository<LoanPayment, UUID> {

    List<LoanPayment> findByLoanApplicationIdOrderByPaymentDate(UUID loanApplicationId);
}

