package com.banking.kyc.web.dto;

import com.banking.kyc.domain.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VerifyDocumentRequest(
        @NotNull VerificationStatus verificationStatus,
        UUID verifiedBy,
        String verificationNotes
) {
}

