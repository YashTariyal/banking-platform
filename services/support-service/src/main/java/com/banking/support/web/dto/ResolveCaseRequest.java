package com.banking.support.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveCaseRequest(
        @NotBlank(message = "Resolution notes are required")
        @Size(max = 5000, message = "Resolution notes must not exceed 5000 characters")
        String resolutionNotes
) {
}

