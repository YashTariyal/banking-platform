package com.banking.compliance.web.dto;

import com.banking.compliance.domain.ReportType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GenerateReportRequest(
        @NotNull ReportType reportType,
        @NotNull LocalDate reportPeriodStart,
        @NotNull LocalDate reportPeriodEnd
) {
}

