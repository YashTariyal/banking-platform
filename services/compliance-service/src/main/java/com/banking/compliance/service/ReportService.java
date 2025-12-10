package com.banking.compliance.service;

import com.banking.compliance.domain.ReportStatus;
import com.banking.compliance.domain.ReportType;
import com.banking.compliance.domain.RegulatoryReport;
import com.banking.compliance.repository.ComplianceRecordRepository;
import com.banking.compliance.repository.RegulatoryReportRepository;
import com.banking.compliance.repository.SuspiciousActivityRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final RegulatoryReportRepository regulatoryReportRepository;
    private final ComplianceRecordRepository complianceRecordRepository;
    private final SuspiciousActivityRepository suspiciousActivityRepository;

    public ReportService(
            RegulatoryReportRepository regulatoryReportRepository,
            ComplianceRecordRepository complianceRecordRepository,
            SuspiciousActivityRepository suspiciousActivityRepository
    ) {
        this.regulatoryReportRepository = regulatoryReportRepository;
        this.complianceRecordRepository = complianceRecordRepository;
        this.suspiciousActivityRepository = suspiciousActivityRepository;
    }

    public Page<RegulatoryReport> getReports(Pageable pageable) {
        return regulatoryReportRepository.findAll(pageable);
    }

    public Page<RegulatoryReport> getReportsByType(ReportType reportType, Pageable pageable) {
        return regulatoryReportRepository.findByReportType(reportType, pageable);
    }

    public Page<RegulatoryReport> getReportsByStatus(ReportStatus status, Pageable pageable) {
        return regulatoryReportRepository.findByStatus(status, pageable);
    }

    public RegulatoryReport getReport(UUID id) {
        return regulatoryReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Regulatory report not found: " + id));
    }

    @Transactional
    public RegulatoryReport generateReport(
            ReportType reportType,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        // Check if report already exists for this period
        Optional<RegulatoryReport> existing = regulatoryReportRepository
                .findByReportTypeAndReportPeriodStartAndReportPeriodEnd(reportType, periodStart, periodEnd);

        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                    "Report already exists for type " + reportType + " and period " + periodStart + " to " + periodEnd
            );
        }

        RegulatoryReport report = new RegulatoryReport();
        report.setReportType(reportType);
        report.setReportPeriodStart(periodStart);
        report.setReportPeriodEnd(periodEnd);
        report.setStatus(ReportStatus.DRAFT);

        // Calculate report statistics based on type
        switch (reportType) {
            case SAR -> generateSARStatistics(report, periodStart, periodEnd);
            case CTR -> generateCTRStatistics(report, periodStart, periodEnd);
            case LCTR -> generateLCTRStatistics(report, periodStart, periodEnd);
            default -> {
                // Default statistics
                report.setRecordCount(0);
                report.setTotalAmount(BigDecimal.ZERO);
            }
        }

        return regulatoryReportRepository.save(report);
    }

    @Transactional
    public RegulatoryReport submitReport(UUID id, UUID submittedBy, String regulatoryReference) {
        RegulatoryReport report = getReport(id);
        if (report.getStatus() != ReportStatus.DRAFT && report.getStatus() != ReportStatus.GENERATED) {
            throw new IllegalStateException("Report cannot be submitted in status: " + report.getStatus());
        }

        report.setStatus(ReportStatus.SUBMITTED);
        report.setSubmittedAt(Instant.now());
        report.setSubmittedBy(submittedBy);
        report.setRegulatoryReference(regulatoryReference);

        return regulatoryReportRepository.save(report);
    }

    private void generateSARStatistics(RegulatoryReport report, LocalDate start, LocalDate end) {
        Instant startInstant = start.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant endInstant = end.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC);

        List<com.banking.compliance.domain.SuspiciousActivity> activities =
                suspiciousActivityRepository.findByDateRange(startInstant, endInstant);

        report.setRecordCount(activities.size());
        report.setTotalAmount(activities.stream()
                .filter(a -> a.getAmount() != null)
                .map(com.banking.compliance.domain.SuspiciousActivity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void generateCTRStatistics(RegulatoryReport report, LocalDate start, LocalDate end) {
        Instant startInstant = start.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant endInstant = end.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC);

        List<com.banking.compliance.domain.ComplianceRecord> records =
                complianceRecordRepository.findAll().stream()
                        .filter(r -> r.getCreatedAt().isAfter(startInstant) && r.getCreatedAt().isBefore(endInstant))
                        .filter(r -> r.getAmount() != null && r.getAmount().compareTo(new BigDecimal("10000")) >= 0)
                        .toList();

        report.setRecordCount(records.size());
        report.setTotalAmount(records.stream()
                .map(com.banking.compliance.domain.ComplianceRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void generateLCTRStatistics(RegulatoryReport report, LocalDate start, LocalDate end) {
        // Similar to CTR but for large cash transactions specifically
        generateCTRStatistics(report, start, end);
    }
}

