package com.banking.compliance.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.compliance.domain.ReportStatus;
import com.banking.compliance.domain.ReportType;
import com.banking.compliance.domain.RegulatoryReport;
import com.banking.compliance.service.ReportService;
import com.banking.compliance.web.dto.GenerateReportRequest;
import com.banking.compliance.web.dto.RegulatoryReportResponse;
import com.banking.compliance.web.dto.SubmitReportRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RegulatoryReportController.class)
class RegulatoryReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private ComplianceMapper complianceMapper;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void generateReport_CreatesNewReport() throws Exception {
        // Given
        GenerateReportRequest request = new GenerateReportRequest(
                ReportType.SAR,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31)
        );

        RegulatoryReport report = createRegulatoryReport();
        report.setReportType(ReportType.SAR);
        report.setReportPeriodStart(request.reportPeriodStart());
        report.setReportPeriodEnd(request.reportPeriodEnd());

        RegulatoryReportResponse response = createRegulatoryReportResponse(report);

        when(reportService.generateReport(
                eq(ReportType.SAR),
                eq(request.reportPeriodStart()),
                eq(request.reportPeriodEnd())))
                .thenReturn(report);
        when(complianceMapper.toResponse(report)).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/api/compliance/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportType").value(ReportType.SAR.name()))
                .andExpect(jsonPath("$.reportPeriodStart").value("2024-01-01"))
                .andExpect(jsonPath("$.reportPeriodEnd").value("2024-01-31"));

        verify(reportService).generateReport(
                ReportType.SAR,
                request.reportPeriodStart(),
                request.reportPeriodEnd()
        );
    }

    @Test
    void getReport_WhenExists_ReturnsReport() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        RegulatoryReport report = createRegulatoryReport();
        report.setId(id);
        RegulatoryReportResponse response = createRegulatoryReportResponse(report);

        when(reportService.getReport(id)).thenReturn(report);
        when(complianceMapper.toResponse(report)).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/api/compliance/reports/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.reportType").value(report.getReportType().name()));
    }

    @Test
    void listReports_ReturnsPaginatedResults() throws Exception {
        // Given
        RegulatoryReport report = createRegulatoryReport();
        Page<RegulatoryReport> page = new PageImpl<>(List.of(report));
        RegulatoryReportResponse response = createRegulatoryReportResponse(report);

        when(reportService.getReports(any())).thenReturn(page);
        when(complianceMapper.toResponse(report)).thenReturn(response);
        when(complianceMapper.toPageResponse(any(org.springframework.data.domain.Page.class))).thenAnswer(invocation -> {
            org.springframework.data.domain.Page<?> p = invocation.getArgument(0);
            return new com.banking.compliance.web.dto.PageResponse<>(
                    p.getContent(),
                    p.getNumber(),
                    p.getSize(),
                    p.getTotalElements(),
                    p.getTotalPages(),
                    p.hasNext(),
                    p.hasPrevious()
            );
        });

        // When/Then
        mockMvc.perform(get("/api/compliance/reports")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listReports_WithTypeFilter_ReturnsFilteredResults() throws Exception {
        // Given
        ReportType reportType = ReportType.SAR;
        RegulatoryReport report = createRegulatoryReport();
        report.setReportType(reportType);
        Page<RegulatoryReport> page = new PageImpl<>(List.of(report));
        RegulatoryReportResponse response = createRegulatoryReportResponse(report);

        when(reportService.getReportsByType(eq(reportType), any())).thenReturn(page);
        when(complianceMapper.toResponse(report)).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/api/compliance/reports")
                        .param("reportType", reportType.name())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void listReports_WithStatusFilter_ReturnsFilteredResults() throws Exception {
        // Given
        ReportStatus status = ReportStatus.DRAFT;
        RegulatoryReport report = createRegulatoryReport();
        report.setStatus(status);
        Page<RegulatoryReport> page = new PageImpl<>(List.of(report));
        RegulatoryReportResponse response = createRegulatoryReportResponse(report);

        when(reportService.getReportsByStatus(eq(status), any())).thenReturn(page);
        when(complianceMapper.toResponse(report)).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/api/compliance/reports")
                        .param("status", status.name())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void submitReport_SubmitsReportSuccessfully() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        UUID submittedBy = UUID.randomUUID();
        String regulatoryReference = "REF-12345";

        SubmitReportRequest request = new SubmitReportRequest(submittedBy, regulatoryReference);

        RegulatoryReport report = createRegulatoryReport();
        report.setId(id);
        report.setStatus(ReportStatus.SUBMITTED);
        report.setSubmittedBy(submittedBy);
        report.setRegulatoryReference(regulatoryReference);
        report.setSubmittedAt(Instant.now());

        RegulatoryReportResponse response = createRegulatoryReportResponse(report);

        when(reportService.submitReport(id, submittedBy, regulatoryReference))
                .thenReturn(report);
        when(complianceMapper.toResponse(report)).thenReturn(response);

        // When/Then
        mockMvc.perform(put("/api/compliance/reports/{id}/submit", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReportStatus.SUBMITTED.name()))
                .andExpect(jsonPath("$.regulatoryReference").value(regulatoryReference));

        verify(reportService).submitReport(id, submittedBy, regulatoryReference);
    }

    private RegulatoryReport createRegulatoryReport() {
        RegulatoryReport report = new RegulatoryReport();
        report.setId(UUID.randomUUID());
        report.setReportType(ReportType.SAR);
        report.setReportPeriodStart(LocalDate.of(2024, 1, 1));
        report.setReportPeriodEnd(LocalDate.of(2024, 1, 31));
        report.setStatus(ReportStatus.DRAFT);
        report.setRecordCount(10);
        report.setTotalAmount(new BigDecimal("50000.00"));
        report.setCreatedAt(Instant.now());
        report.setUpdatedAt(Instant.now());
        return report;
    }

    private RegulatoryReportResponse createRegulatoryReportResponse(RegulatoryReport report) {
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
}

