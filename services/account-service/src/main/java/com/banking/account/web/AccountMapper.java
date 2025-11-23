package com.banking.account.web;

import com.banking.account.domain.Account;
import com.banking.account.domain.AccountTransactionLog;
import com.banking.account.web.dto.AccountResponse;
import com.banking.account.web.dto.BalanceResponse;
import com.banking.account.web.dto.TransactionHistoryResponse;

public final class AccountMapper {

    private AccountMapper() {
    }

    public static AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCustomerId(),
                account.getType(),
                account.getStatus(),
                account.getCurrency(),
                account.getBalance(),
                account.getOpenedAt(),
                account.getUpdatedAt()
        );
    }

    public static BalanceResponse toBalanceResponse(Account account) {
        return new BalanceResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency()
        );
    }

    public static TransactionHistoryResponse toTransactionHistoryResponse(AccountTransactionLog log) {
        return new TransactionHistoryResponse(
                log.getReferenceId(),
                log.getType(),
                log.getAmount(),
                log.getResultingBalance(),
                log.getCreatedAt()
        );
    }
}

