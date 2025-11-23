package com.banking.account.messaging;

import com.banking.account.domain.Account;
import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountEvent(
        UUID accountId,
        String accountNumber,
        UUID customerId,
        AccountType type,
        AccountStatus status,
        String currency,
        BigDecimal balance,
        String eventType,
        Instant occurredAt
) {

    public static AccountEvent created(Account account) {
        return fromAccount(account, "ACCOUNT_CREATED");
    }

    public static AccountEvent updated(Account account) {
        return fromAccount(account, "ACCOUNT_UPDATED");
    }

    private static AccountEvent fromAccount(Account account, String eventType) {
        return new AccountEvent(
                account.getId(),
                account.getAccountNumber(),
                account.getCustomerId(),
                account.getType(),
                account.getStatus(),
                account.getCurrency(),
                account.getBalance(),
                eventType,
                Instant.now()
        );
    }
}

