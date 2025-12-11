package com.banking.payment.web.dto;

import com.banking.payment.domain.PaymentDirection;
import com.banking.payment.domain.PaymentRail;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotBlank(message = "Reference ID is required")
        @Size(max = 255, message = "Reference ID must not exceed 255 characters")
        String referenceId,

        @NotNull(message = "Payment rail is required")
        PaymentRail rail,

        @NotNull(message = "Payment direction is required")
        PaymentDirection direction,

        @NotNull(message = "From account ID is required")
        UUID fromAccountId,

        UUID toAccountId,

        @Size(max = 255, message = "External account must not exceed 255 characters")
        String toExternalAccount,

        @Size(max = 255, message = "External routing must not exceed 255 characters")
        String toExternalRouting,

        @Size(max = 255, message = "External bank name must not exceed 255 characters")
        String toExternalBankName,

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

