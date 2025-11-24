package com.banking.account.repository;

import com.banking.account.domain.AccountGoalContribution;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountGoalContributionRepository extends JpaRepository<AccountGoalContribution, UUID> {

    List<AccountGoalContribution> findByGoalId(UUID goalId);
}

