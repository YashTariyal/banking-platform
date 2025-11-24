package com.banking.account.web.dto;

import com.banking.account.domain.AccountGoalCadence;
import com.banking.account.domain.AccountGoalStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AccountGoalResponse(
        UUID id,
        UUID accountId,
        String name,
        String description,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        BigDecimal remainingAmount,
        BigDecimal progressPercentage,
        AccountGoalStatus status,
        LocalDate dueDate,
        boolean autoSweepEnabled,
        AccountGoalCadence autoSweepCadence,
        BigDecimal autoSweepAmount,
        Instant lastSweepAt,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {
}

