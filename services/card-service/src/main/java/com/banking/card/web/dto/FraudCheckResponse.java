package com.banking.card.web.dto;

import com.banking.card.domain.FraudSeverity;
import java.math.BigDecimal;
import java.util.List;

public record FraudCheckResponse(
        boolean isFraudulent,
        BigDecimal fraudScore,
        FraudSeverity severity,
        List<String> riskFactors
) {
}

