package com.banking.account.web.dto;

import com.banking.account.domain.AccountGoalCadence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateAccountGoalRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 1024) String description,
        @NotNull @Positive BigDecimal targetAmount,
        LocalDate dueDate,
        Boolean autoSweepEnabled,
        @Positive BigDecimal autoSweepAmount,
        AccountGoalCadence autoSweepCadence
) {
}

