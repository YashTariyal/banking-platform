package com.banking.payment.repository;

import com.banking.payment.domain.Payment;
import com.banking.payment.domain.PaymentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByReferenceId(String referenceId);

    Page<Payment> findByFromAccountId(UUID fromAccountId, Pageable pageable);

    Page<Payment> findByToAccountId(UUID toAccountId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE (p.fromAccountId = :accountId OR p.toAccountId = :accountId)")
    Page<Payment> findByAccountId(@Param("accountId") UUID accountId, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.status IN :statuses")
    Page<Payment> findByStatusIn(@Param("statuses") List<PaymentStatus> statuses, Pageable pageable);
}

