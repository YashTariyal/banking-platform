package com.banking.kyc.web.dto;

import com.banking.kyc.domain.DocumentType;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateDocumentRequest(
        @NotNull UUID kycCaseId,
        @NotNull DocumentType documentType,
        @NotNull String fileName,
        String filePath,
        Long fileSize,
        String mimeType,
        String documentNumber,
        String issuingCountry,
        Instant expiryDate
) {
}

