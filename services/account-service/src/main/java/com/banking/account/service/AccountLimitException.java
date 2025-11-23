package com.banking.account.service;

import java.math.BigDecimal;
import java.util.UUID;

public class AccountLimitException extends RuntimeException {

    public AccountLimitException(String message) {
        super(message);
    }

    public static AccountLimitException minBalanceViolation(UUID accountId, BigDecimal balance, BigDecimal minBalance) {
        return new AccountLimitException(
                String.format("Account %s balance %.2f is below minimum required balance %.2f", 
                        accountId, balance, minBalance)
        );
    }

    public static AccountLimitException maxBalanceViolation(UUID accountId, BigDecimal balance, BigDecimal maxBalance) {
        return new AccountLimitException(
                String.format("Account %s balance %.2f exceeds maximum allowed balance %.2f", 
                        accountId, balance, maxBalance)
        );
    }

    public static AccountLimitException maxTransactionAmountViolation(BigDecimal amount, BigDecimal maxAmount) {
        return new AccountLimitException(
                String.format("Transaction amount %.2f exceeds maximum allowed amount %.2f", 
                        amount, maxAmount)
        );
    }

    public static AccountLimitException maxDailyTransactionsViolation(int count, int maxCount) {
        return new AccountLimitException(
                String.format("Daily transaction count %d exceeds maximum allowed %d", 
                        count, maxCount)
        );
    }

    public static AccountLimitException maxDailyTransactionAmountViolation(BigDecimal amount, BigDecimal maxAmount) {
        return new AccountLimitException(
                String.format("Daily transaction amount %.2f exceeds maximum allowed %.2f", 
                        amount, maxAmount)
        );
    }
}

