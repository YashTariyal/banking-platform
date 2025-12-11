package com.banking.ledger.web.dto;

import com.banking.ledger.domain.LedgerAccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateLedgerAccountRequest {

    @NotBlank
    @Size(max = 128)
    private String name;

    @NotNull
    private LedgerAccountType type;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @Size(max = 64)
    private String externalAccountId;

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
}

