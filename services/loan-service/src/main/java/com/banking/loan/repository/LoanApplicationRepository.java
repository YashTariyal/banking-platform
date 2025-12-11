package com.banking.loan.repository;

import com.banking.loan.domain.LoanApplication;
import com.banking.loan.domain.LoanStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    Page<LoanApplication> findByCustomerId(UUID customerId, Pageable pageable);

    List<LoanApplication> findByStatus(LoanStatus status);

    Optional<LoanApplication> findByIdAndStatus(UUID id, LoanStatus status);
}

