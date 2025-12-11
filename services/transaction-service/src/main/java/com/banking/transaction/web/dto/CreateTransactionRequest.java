package com.banking.transaction.web.dto;

import com.banking.transaction.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequest(
        @Size(max = 100, message = "Reference ID must not exceed 100 characters")
        String referenceId,

        @NotNull(message = "Transaction type is required")
        TransactionType transactionType,

        @NotNull(message = "Customer ID is required")
        UUID customerId,

        UUID fromAccountId,

        UUID toAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be 3 characters")
        String currency,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}

