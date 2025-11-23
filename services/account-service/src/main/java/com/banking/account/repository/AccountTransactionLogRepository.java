package com.banking.account.repository;

import com.banking.account.domain.AccountTransactionLog;
import com.banking.account.domain.AccountTransactionLogId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountTransactionLogRepository extends JpaRepository<AccountTransactionLog, AccountTransactionLogId> {

    Optional<AccountTransactionLog> findByAccountIdAndReferenceId(UUID accountId, UUID referenceId);

    Page<AccountTransactionLog> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    List<AccountTransactionLog> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    @Query("SELECT COUNT(t) FROM AccountTransactionLog t WHERE t.accountId = :accountId AND t.createdAt >= :startTime AND t.createdAt < :endTime")
    long countByAccountIdAndDateRange(@Param("accountId") UUID accountId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM AccountTransactionLog t WHERE t.accountId = :accountId AND t.createdAt >= :startTime AND t.createdAt < :endTime")
    BigDecimal sumAmountByAccountIdAndDateRange(@Param("accountId") UUID accountId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query("SELECT COUNT(t) FROM AccountTransactionLog t WHERE t.createdAt < :threshold")
    long countByCreatedAtBefore(@Param("threshold") Instant threshold);

    @Query("SELECT t FROM AccountTransactionLog t WHERE t.createdAt < :threshold")
    List<AccountTransactionLog> findByCreatedAtBefore(@Param("threshold") Instant threshold);
}

