package com.banking.account.repository;

import com.banking.account.domain.AccountGoal;
import com.banking.account.domain.AccountGoalStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountGoalRepository extends JpaRepository<AccountGoal, UUID> {

    Page<AccountGoal> findByAccountId(UUID accountId, Pageable pageable);

    Optional<AccountGoal> findByIdAndAccountId(UUID goalId, UUID accountId);

    Page<AccountGoal> findByAutoSweepEnabledTrueAndStatus(AccountGoalStatus status, Pageable pageable);
}

