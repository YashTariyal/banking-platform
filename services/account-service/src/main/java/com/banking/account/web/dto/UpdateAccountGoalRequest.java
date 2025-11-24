package com.banking.account.web.dto;

import com.banking.account.domain.AccountGoalCadence;
import com.banking.account.domain.AccountGoalStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateAccountGoalRequest(
        @Size(max = 128) String name,
        @Size(max = 1024) String description,
        @Positive BigDecimal targetAmount,
        LocalDate dueDate,
        Boolean autoSweepEnabled,
        @Positive BigDecimal autoSweepAmount,
        AccountGoalCadence autoSweepCadence,
        AccountGoalStatus status
) {
}

