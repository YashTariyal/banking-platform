package com.banking.kyc.web.dto;

import com.banking.kyc.domain.ScreeningResultStatus;
import com.banking.kyc.domain.ScreeningType;
import java.time.Instant;
import java.util.UUID;

public record ScreeningResultResponse(
        UUID id,
        UUID kycCaseId,
        UUID customerId,
        ScreeningType screeningType,
        ScreeningResultStatus result,
        Integer matchScore,
        String matchedName,
        String matchedList,
        String matchDetails,
        String screeningProvider,
        String screeningReference,
        Instant reviewedAt,
        UUID reviewedBy,
        String reviewNotes,
        Instant createdAt,
        Instant updatedAt
) {
}

