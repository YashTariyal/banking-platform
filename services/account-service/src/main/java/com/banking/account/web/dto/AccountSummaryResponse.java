package com.banking.account.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccountSummaryResponse(
        UUID accountId,
        String accountNumber,
        BigDecimal balance,
        String currency,
        int recentTransactionCount,
        List<RecentTransaction> recentTransactions,
        List<GoalSnapshot> goals
) {

    public record RecentTransaction(
            UUID referenceId,
            String type,
            BigDecimal amount,
            BigDecimal resultingBalance,
            String description,
            Instant createdAt
    ) {
    }

    public record GoalSnapshot(
            UUID goalId,
            String name,
            BigDecimal currentAmount,
            BigDecimal targetAmount,
            String status,
            Instant dueDate
    ) {
    }
}


