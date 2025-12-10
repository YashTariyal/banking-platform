package com.banking.compliance.web.dto;

import com.banking.compliance.domain.SuspiciousActivityStatus;
import java.util.UUID;

public record UpdateSuspiciousActivityRequest(
        SuspiciousActivityStatus status,
        UUID investigatorId,
        String investigationNotes
) {
}

