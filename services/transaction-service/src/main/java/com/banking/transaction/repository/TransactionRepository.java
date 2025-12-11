package com.banking.transaction.repository;

import com.banking.transaction.domain.Transaction;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.domain.TransactionType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByReferenceId(String referenceId);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    Page<Transaction> findByTransactionType(TransactionType transactionType, Pageable pageable);

    Page<Transaction> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Transaction> findByFromAccountId(UUID fromAccountId, Pageable pageable);

    Page<Transaction> findByToAccountId(UUID toAccountId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.fromAccountId = :accountId OR t.toAccountId = :accountId")
    Page<Transaction> findByAccountId(@Param("accountId") UUID accountId, Pageable pageable);
}

