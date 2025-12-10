package com.banking.identity.web.dto;

import com.banking.identity.domain.MFAMethod;
import java.time.Instant;
import java.util.UUID;

public record MFASettingsResponse(
        UUID id,
        UUID userId,
        Boolean mfaEnabled,
        MFAMethod mfaMethod,
        Boolean phoneVerified,
        Instant createdAt,
        Instant updatedAt
) {
}

