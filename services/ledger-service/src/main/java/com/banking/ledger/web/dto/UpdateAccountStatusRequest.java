package com.banking.ledger.web.dto;

import com.banking.ledger.domain.LedgerAccountStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateAccountStatusRequest {

    @NotNull
    private LedgerAccountStatus status;

    public LedgerAccountStatus getStatus() {
        return status;
    }

    public void setStatus(LedgerAccountStatus status) {
        this.status = status;
    }
}

