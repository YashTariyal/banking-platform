package com.banking.loan.repository;

import com.banking.loan.domain.PaymentScheduleItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentScheduleItemRepository extends JpaRepository<PaymentScheduleItem, UUID> {

    List<PaymentScheduleItem> findByLoanApplicationIdOrderByInstallmentNumber(UUID loanApplicationId);
}

