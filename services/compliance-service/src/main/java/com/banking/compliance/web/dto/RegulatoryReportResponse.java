package com.banking.compliance.web.dto;

import com.banking.compliance.domain.ReportStatus;
import com.banking.compliance.domain.ReportType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RegulatoryReportResponse(
        UUID id,
        ReportType reportType,
        LocalDate reportPeriodStart,
        LocalDate reportPeriodEnd,
        ReportStatus status,
        String filePath,
        Integer recordCount,
        BigDecimal totalAmount,
        Instant submittedAt,
        UUID submittedBy,
        String regulatoryReference,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}

