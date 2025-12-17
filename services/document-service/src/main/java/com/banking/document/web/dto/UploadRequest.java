package com.banking.document.web.dto;

import com.banking.document.domain.Document.DocumentCategory;
import com.banking.document.domain.Document.DocumentType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UploadRequest(
        @NotNull UUID customerId,
        UUID accountId,
        @NotNull DocumentType documentType,
        @NotNull DocumentCategory category,
        String description,
        UUID uploadedBy
) {}
