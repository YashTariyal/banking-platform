package com.banking.account.repository;

import com.banking.account.domain.AccountGoal;
import com.banking.account.domain.AccountGoalStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountGoalRepository extends JpaRepository<AccountGoal, UUID> {

    Page<AccountGoal> findByAccountId(UUID accountId, Pageable pageable);

    Optional<AccountGoal> findByIdAndAccountId(UUID goalId, UUID accountId);

    @Query("""
            SELECT g FROM AccountGoal g
            WHERE g.autoSweepEnabled = true
              AND g.status = :status
              AND (g.nextSweepAt IS NULL OR g.nextSweepAt <= :runAt)
            ORDER BY g.nextSweepAt
            """)
    Page<AccountGoal> findDueGoals(
            @Param("status") AccountGoalStatus status,
            @Param("runAt") Instant runAt,
            Pageable pageable
    );
}

