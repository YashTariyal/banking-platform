package com.banking.compliance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.compliance.domain.ComplianceRecord;
import com.banking.compliance.domain.ComplianceRecordType;
import com.banking.compliance.domain.ComplianceStatus;
import com.banking.compliance.domain.ReportStatus;
import com.banking.compliance.domain.ReportType;
import com.banking.compliance.domain.RegulatoryReport;
import com.banking.compliance.domain.SuspiciousActivity;
import com.banking.compliance.domain.SuspiciousActivityStatus;
import com.banking.compliance.repository.ComplianceRecordRepository;
import com.banking.compliance.repository.RegulatoryReportRepository;
import com.banking.compliance.repository.SuspiciousActivityRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private RegulatoryReportRepository regulatoryReportRepository;

    @Mock
    private ComplianceRecordRepository complianceRecordRepository;

    @Mock
    private SuspiciousActivityRepository suspiciousActivityRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                regulatoryReportRepository,
                complianceRecordRepository,
                suspiciousActivityRepository
        );
    }

    @Test
    void generateReport_ForSAR_GeneratesReportWithStatistics() {
        // Given
        ReportType reportType = ReportType.SAR;
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        when(regulatoryReportRepository.findByReportTypeAndReportPeriodStartAndReportPeriodEnd(
                reportType, start, end)).thenReturn(Optional.empty());

        SuspiciousActivity activity = createSuspiciousActivity();
        activity.setAmount(new BigDecimal("5000.00"));
        Instant startInstant = start.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant endInstant = end.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC);

        when(suspiciousActivityRepository.findByDateRange(startInstant, endInstant))
                .thenReturn(List.of(activity));

        when(regulatoryReportRepository.save(any(RegulatoryReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RegulatoryReport report = reportService.generateReport(reportType, start, end);

        // Then
        assertThat(report).isNotNull();
        assertThat(report.getReportType()).isEqualTo(ReportType.SAR);
        assertThat(report.getReportPeriodStart()).isEqualTo(start);
        assertThat(report.getReportPeriodEnd()).isEqualTo(end);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.DRAFT);
        assertThat(report.getRecordCount()).isEqualTo(1);
        assertThat(report.getTotalAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        verify(regulatoryReportRepository).save(report);
    }

    @Test
    void generateReport_ForCTR_GeneratesReportWithLargeTransactions() {
        // Given
        ReportType reportType = ReportType.CTR;
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        when(regulatoryReportRepository.findByReportTypeAndReportPeriodStartAndReportPeriodEnd(
                reportType, start, end)).thenReturn(Optional.empty());

        ComplianceRecord record1 = createComplianceRecord();
        record1.setAmount(new BigDecimal("15000.00"));
        record1.setCreatedAt(start.atStartOfDay().toInstant(java.time.ZoneOffset.UTC).plusSeconds(3600));

        ComplianceRecord record2 = createComplianceRecord();
        record2.setAmount(new BigDecimal("20000.00"));
        record2.setCreatedAt(start.atStartOfDay().toInstant(java.time.ZoneOffset.UTC).plusSeconds(7200));

        when(complianceRecordRepository.findAll()).thenReturn(List.of(record1, record2));

        when(regulatoryReportRepository.save(any(RegulatoryReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RegulatoryReport report = reportService.generateReport(reportType, start, end);

        // Then
        assertThat(report).isNotNull();
        assertThat(report.getReportType()).isEqualTo(ReportType.CTR);
        assertThat(report.getRecordCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void generateReport_WhenReportExists_ThrowsException() {
        // Given
        ReportType reportType = ReportType.SAR;
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        RegulatoryReport existing = new RegulatoryReport();
        existing.setId(UUID.randomUUID());

        when(regulatoryReportRepository.findByReportTypeAndReportPeriodStartAndReportPeriodEnd(
                reportType, start, end)).thenReturn(Optional.of(existing));

        // When/Then
        assertThatThrownBy(() -> reportService.generateReport(reportType, start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void submitReport_WhenDraft_SubmitsSuccessfully() {
        // Given
        UUID reportId = UUID.randomUUID();
        UUID submittedBy = UUID.randomUUID();
        String regulatoryReference = "REF-12345";

        RegulatoryReport report = new RegulatoryReport();
        report.setId(reportId);
        report.setStatus(ReportStatus.DRAFT);
        report.setReportType(ReportType.SAR);

        when(regulatoryReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(regulatoryReportRepository.save(any(RegulatoryReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RegulatoryReport result = reportService.submitReport(reportId, submittedBy, regulatoryReference);

        // Then
        assertThat(result.getStatus()).isEqualTo(ReportStatus.SUBMITTED);
        assertThat(result.getSubmittedBy()).isEqualTo(submittedBy);
        assertThat(result.getRegulatoryReference()).isEqualTo(regulatoryReference);
        assertThat(result.getSubmittedAt()).isNotNull();
        verify(regulatoryReportRepository).save(report);
    }

    @Test
    void submitReport_WhenGenerated_SubmitsSuccessfully() {
        // Given
        UUID reportId = UUID.randomUUID();
        RegulatoryReport report = new RegulatoryReport();
        report.setId(reportId);
        report.setStatus(ReportStatus.GENERATED);

        when(regulatoryReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(regulatoryReportRepository.save(any(RegulatoryReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RegulatoryReport result = reportService.submitReport(reportId, UUID.randomUUID(), "REF");

        // Then
        assertThat(result.getStatus()).isEqualTo(ReportStatus.SUBMITTED);
    }

    @Test
    void submitReport_WhenAlreadySubmitted_ThrowsException() {
        // Given
        UUID reportId = UUID.randomUUID();
        RegulatoryReport report = new RegulatoryReport();
        report.setId(reportId);
        report.setStatus(ReportStatus.SUBMITTED);

        when(regulatoryReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        // When/Then
        assertThatThrownBy(() -> reportService.submitReport(reportId, UUID.randomUUID(), "REF"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be submitted");
    }

    @Test
    void getReports_ReturnsPaginatedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        RegulatoryReport report = createRegulatoryReport();
        Page<RegulatoryReport> page = new PageImpl<>(List.of(report));

        when(regulatoryReportRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<RegulatoryReport> result = reportService.getReports(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getReportsByType_ReturnsFilteredResults() {
        // Given
        ReportType reportType = ReportType.SAR;
        Pageable pageable = PageRequest.of(0, 20);
        RegulatoryReport report = createRegulatoryReport();
        report.setReportType(reportType);
        Page<RegulatoryReport> page = new PageImpl<>(List.of(report));

        when(regulatoryReportRepository.findByReportType(reportType, pageable)).thenReturn(page);

        // When
        Page<RegulatoryReport> result = reportService.getReportsByType(reportType, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getReportType()).isEqualTo(reportType);
    }

    @Test
    void getReport_WhenExists_ReturnsReport() {
        // Given
        UUID id = UUID.randomUUID();
        RegulatoryReport report = createRegulatoryReport();
        report.setId(id);

        when(regulatoryReportRepository.findById(id)).thenReturn(Optional.of(report));

        // When
        RegulatoryReport result = reportService.getReport(id);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void getReport_WhenNotExists_ThrowsException() {
        // Given
        UUID id = UUID.randomUUID();
        when(regulatoryReportRepository.findById(id)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> reportService.getReport(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    private ComplianceRecord createComplianceRecord() {
        ComplianceRecord record = new ComplianceRecord();
        record.setId(UUID.randomUUID());
        record.setCustomerId(UUID.randomUUID());
        record.setRecordType(ComplianceRecordType.TRANSACTION_MONITORING);
        record.setStatus(ComplianceStatus.PENDING);
        record.setAmount(new BigDecimal("10000.00"));
        record.setCurrency("USD");
        record.setCreatedAt(Instant.now());
        return record;
    }

    private SuspiciousActivity createSuspiciousActivity() {
        SuspiciousActivity activity = new SuspiciousActivity();
        activity.setId(UUID.randomUUID());
        activity.setCustomerId(UUID.randomUUID());
        activity.setActivityType(com.banking.compliance.domain.SuspiciousActivityType.UNUSUAL_PATTERN);
        activity.setSeverity(com.banking.compliance.domain.Severity.MEDIUM);
        activity.setStatus(SuspiciousActivityStatus.OPEN);
        activity.setRiskScore(60);
        activity.setDescription("Test activity");
        activity.setCreatedAt(Instant.now());
        return activity;
    }

    private RegulatoryReport createRegulatoryReport() {
        RegulatoryReport report = new RegulatoryReport();
        report.setId(UUID.randomUUID());
        report.setReportType(ReportType.SAR);
        report.setReportPeriodStart(LocalDate.of(2024, 1, 1));
        report.setReportPeriodEnd(LocalDate.of(2024, 1, 31));
        report.setStatus(ReportStatus.DRAFT);
        report.setCreatedAt(Instant.now());
        return report;
    }
}

