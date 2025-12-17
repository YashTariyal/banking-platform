package com.banking.document.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record StatementRequest(
        @NotNull UUID customerId,
        @NotNull UUID accountId,
        @NotNull String accountNumber,
        @NotNull String customerName,
        @NotNull String accountType,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd
) {}
