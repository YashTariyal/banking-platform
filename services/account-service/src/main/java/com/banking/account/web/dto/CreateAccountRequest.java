package com.banking.account.web.dto;

import com.banking.account.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountRequest(
        @NotNull UUID customerId,
        @NotNull AccountType type,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be ISO 4217 code") String currency,
        @NotNull @PositiveOrZero BigDecimal initialDeposit
) {
}

