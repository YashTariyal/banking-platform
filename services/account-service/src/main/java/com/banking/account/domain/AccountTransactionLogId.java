package com.banking.account.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class AccountTransactionLogId implements Serializable {

    private UUID accountId;
    private UUID referenceId;

    public AccountTransactionLogId() {
    }

    public AccountTransactionLogId(UUID accountId, UUID referenceId) {
        this.accountId = accountId;
        this.referenceId = referenceId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountTransactionLogId that = (AccountTransactionLogId) o;
        return Objects.equals(accountId, that.accountId) && Objects.equals(referenceId, that.referenceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, referenceId);
    }
}

