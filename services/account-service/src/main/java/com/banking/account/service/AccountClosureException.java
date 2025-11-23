package com.banking.account.service;

import java.math.BigDecimal;
import java.util.UUID;

public class AccountClosureException extends RuntimeException {

    public AccountClosureException(UUID accountId, String reason) {
        super(String.format("Cannot close account %s: %s", accountId, reason));
    }

    public static AccountClosureException hasBalance(UUID accountId, BigDecimal balance) {
        return new AccountClosureException(accountId, 
                String.format("Account has non-zero balance: %.2f. Balance must be zero before closure.", balance));
    }
}

