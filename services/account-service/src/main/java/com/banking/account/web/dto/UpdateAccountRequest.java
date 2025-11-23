package com.banking.account.web.dto;

import com.banking.account.domain.AccountType;
import jakarta.validation.constraints.Pattern;

public record UpdateAccountRequest(
        AccountType type,
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be ISO 4217 code") String currency
) {
}

