package com.banking.document.web.dto;

import com.banking.document.domain.Document.DocumentCategory;
import com.banking.document.domain.Document.DocumentStatus;
import com.banking.document.domain.Document.DocumentType;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID customerId,
        UUID accountId,
        DocumentType documentType,
        DocumentCategory category,
        String fileName,
        String contentType,
        Long fileSize,
        DocumentStatus status,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
