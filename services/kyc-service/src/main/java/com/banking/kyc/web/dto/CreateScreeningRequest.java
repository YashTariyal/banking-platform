package com.banking.kyc.web.dto;

import com.banking.kyc.domain.ScreeningResultStatus;
import com.banking.kyc.domain.ScreeningType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateScreeningRequest(
        @NotNull UUID kycCaseId,
        @NotNull UUID customerId,
        @NotNull ScreeningType screeningType,
        ScreeningResultStatus result,
        Integer matchScore,
        String matchedName,
        String matchedList,
        String matchDetails,
        String screeningProvider,
        String screeningReference
) {
}

