package com.banking.account.web;

import com.banking.account.domain.AccountGoal;
import com.banking.account.web.dto.AccountGoalResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AccountGoalMapper {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private AccountGoalMapper() {
    }

    public static AccountGoalResponse toResponse(AccountGoal goal) {
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount());
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }
        BigDecimal progress = BigDecimal.ZERO;
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            progress = goal.getCurrentAmount()
                    .multiply(ONE_HUNDRED)
                    .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
        }
        return new AccountGoalResponse(
                goal.getId(),
                goal.getAccountId(),
                goal.getName(),
                goal.getDescription(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                remaining,
                progress,
                goal.getStatus(),
                goal.getDueDate(),
                goal.isAutoSweepEnabled(),
                goal.getAutoSweepCadence(),
                goal.getAutoSweepAmount(),
                goal.getLastSweepAt(),
                goal.getCreatedAt(),
                goal.getUpdatedAt(),
                goal.getCompletedAt()
        );
    }
}

