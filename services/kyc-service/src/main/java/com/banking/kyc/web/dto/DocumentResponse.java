package com.banking.kyc.web.dto;

import com.banking.kyc.domain.DocumentType;
import com.banking.kyc.domain.VerificationStatus;
import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID kycCaseId,
        DocumentType documentType,
        String fileName,
        String filePath,
        Long fileSize,
        String mimeType,
        VerificationStatus verificationStatus,
        Instant verifiedAt,
        UUID verifiedBy,
        String verificationNotes,
        Instant expiryDate,
        String documentNumber,
        String issuingCountry,
        Instant createdAt,
        Instant updatedAt
) {
}

