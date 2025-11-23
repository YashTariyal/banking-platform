package com.banking.account.web.dto;

import java.util.List;

public record BulkAccountResponse(
        int totalRequested,
        int successful,
        int failed,
        List<AccountResponse> createdAccounts,
        List<BulkOperationError> errors
) {
    public record BulkOperationError(
            int index,
            String reason,
            String message
    ) {
    }
}

