package com.banking.kyc.messaging;

import com.banking.kyc.domain.KYCCase;
import com.banking.kyc.domain.KYCStatus;
import com.banking.kyc.domain.RiskLevel;
import java.time.Instant;
import java.util.UUID;

public record KYCEvent(
        UUID kycCaseId,
        UUID customerId,
        KYCStatus status,
        RiskLevel riskLevel,
        String caseType,
        String eventType,
        Instant occurredAt
) {

    public static KYCEvent created(KYCCase kycCase) {
        return fromKYCCase(kycCase, "KYC_CASE_CREATED");
    }

    public static KYCEvent updated(KYCCase kycCase) {
        return fromKYCCase(kycCase, "KYC_CASE_UPDATED");
    }

    public static KYCEvent approved(KYCCase kycCase) {
        return fromKYCCase(kycCase, "KYC_CASE_APPROVED");
    }

    public static KYCEvent rejected(KYCCase kycCase) {
        return fromKYCCase(kycCase, "KYC_CASE_REJECTED");
    }

    private static KYCEvent fromKYCCase(KYCCase kycCase, String eventType) {
        return new KYCEvent(
                kycCase.getId(),
                kycCase.getCustomerId(),
                kycCase.getStatus(),
                kycCase.getRiskLevel(),
                kycCase.getCaseType(),
                eventType,
                Instant.now()
        );
    }
}

