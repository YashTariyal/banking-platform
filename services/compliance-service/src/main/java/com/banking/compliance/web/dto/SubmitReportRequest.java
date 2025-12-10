package com.banking.compliance.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record SubmitReportRequest(
        UUID submittedBy,
        @NotBlank String regulatoryReference
) {
}

