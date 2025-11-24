package com.banking.account.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record GoalContributionRequest(
        @NotNull @Positive BigDecimal amount,
        UUID referenceId,
        @Size(max = 255) String description
) {
}

