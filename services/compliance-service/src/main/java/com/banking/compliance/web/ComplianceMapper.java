package com.banking.compliance.web;

import com.banking.compliance.domain.ComplianceRecord;
import com.banking.compliance.domain.RegulatoryReport;
import com.banking.compliance.domain.SuspiciousActivity;
import com.banking.compliance.web.dto.ComplianceRecordResponse;
import com.banking.compliance.web.dto.RegulatoryReportResponse;
import com.banking.compliance.web.dto.SuspiciousActivityResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ComplianceMapper {

    public ComplianceRecordResponse toResponse(ComplianceRecord record) {
        return new ComplianceRecordResponse(
                record.getId(),
                record.getCustomerId(),
                record.getAccountId(),
                record.getTransactionId(),
                record.getRecordType(),
                record.getStatus(),
                record.getAmount(),
                record.getCurrency(),
                record.getDescription(),
                record.getRiskScore(),
                record.getFlags(),
                record.getSourceEventType(),
                record.getSourceTopic(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    public SuspiciousActivityResponse toResponse(SuspiciousActivity activity) {
        return new SuspiciousActivityResponse(
                activity.getId(),
                activity.getCustomerId(),
                activity.getAccountId(),
                activity.getTransactionId(),
                activity.getActivityType(),
                activity.getSeverity(),
                activity.getStatus(),
                activity.getAmount(),
                activity.getCurrency(),
                activity.getDescription(),
                activity.getRiskScore(),
                activity.getComplianceRecordId(),
                activity.getInvestigatorId(),
                activity.getInvestigationNotes(),
                activity.getReportedAt(),
                activity.getCreatedAt(),
                activity.getUpdatedAt()
        );
    }

    public RegulatoryReportResponse toResponse(RegulatoryReport report) {
        return new RegulatoryReportResponse(
                report.getId(),
                report.getReportType(),
                report.getReportPeriodStart(),
                report.getReportPeriodEnd(),
                report.getStatus(),
                report.getFilePath(),
                report.getRecordCount(),
                report.getTotalAmount(),
                report.getSubmittedAt(),
                report.getSubmittedBy(),
                report.getRegulatoryReference(),
                report.getNotes(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    public <T> com.banking.compliance.web.dto.PageResponse<T> toPageResponse(Page<T> page) {
        return new com.banking.compliance.web.dto.PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}

