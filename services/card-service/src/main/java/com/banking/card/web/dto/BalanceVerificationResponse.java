package com.banking.card.web.dto;

import java.math.BigDecimal;

public record BalanceVerificationResponse(
        boolean sufficient,
        BigDecimal availableBalance,
        BigDecimal requestedAmount
) {
}

