package com.banking.ledger.web.dto;

import com.banking.ledger.domain.LedgerAccountStatus;
import com.banking.ledger.domain.LedgerAccountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class LedgerAccountResponse {

    private UUID id;
    private String name;
    private LedgerAccountType type;
    private LedgerAccountStatus status;
    private String currency;
    private String externalAccountId;
    private BigDecimal balance;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LedgerAccountType getType() {
        return type;
    }

    public void setType(LedgerAccountType type) {
        this.type = type;
    }

    public LedgerAccountStatus getStatus() {
        return status;
    }

    public void setStatus(LedgerAccountStatus status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getExternalAccountId() {
        return externalAccountId;
    }

    public void setExternalAccountId(String externalAccountId) {
        this.externalAccountId = externalAccountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

