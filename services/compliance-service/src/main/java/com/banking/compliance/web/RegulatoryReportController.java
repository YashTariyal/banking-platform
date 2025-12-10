package com.banking.compliance.web;

import com.banking.compliance.domain.ReportStatus;
import com.banking.compliance.domain.ReportType;
import com.banking.compliance.domain.RegulatoryReport;
import com.banking.compliance.service.ReportService;
import com.banking.compliance.web.dto.GenerateReportRequest;
import com.banking.compliance.web.dto.PageResponse;
import com.banking.compliance.web.dto.RegulatoryReportResponse;
import com.banking.compliance.web.dto.SubmitReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compliance/reports")
@Tag(name = "Regulatory Reports", description = "Regulatory report management operations")
public class RegulatoryReportController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReportService reportService;
    private final ComplianceMapper mapper;

    public RegulatoryReportController(ReportService reportService, ComplianceMapper mapper) {
        this.reportService = reportService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate regulatory report", description = "Generates a new regulatory report for the specified period")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or report already exists")
    })
    public ResponseEntity<RegulatoryReportResponse> generateReport(
            @Valid @RequestBody GenerateReportRequest request
    ) {
        RegulatoryReport report = reportService.generateReport(
                request.reportType(),
                request.reportPeriodStart(),
                request.reportPeriodEnd()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(report));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get regulatory report by ID", description = "Retrieves a specific regulatory report by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report found"),
            @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<RegulatoryReportResponse> getReport(
            @Parameter(description = "Report unique identifier", required = true)
            @PathVariable UUID id
    ) {
        RegulatoryReport report = reportService.getReport(id);
        return ResponseEntity.ok(mapper.toResponse(report));
    }

    @GetMapping
    @Operation(summary = "List regulatory reports", description = "Retrieves paginated regulatory reports with optional filters")
    public ResponseEntity<PageResponse<RegulatoryReportResponse>> listReports(
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int pageSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<RegulatoryReport> reports;

        if (reportType != null && status != null) {
            Page<RegulatoryReport> typeReports = reportService.getReportsByType(reportType, pageable);
            List<RegulatoryReport> filtered = typeReports.getContent().stream()
                    .filter(r -> r.getStatus() == status)
                    .toList();
            reports = new PageImpl<>(filtered, pageable, filtered.size());
        } else if (reportType != null) {
            reports = reportService.getReportsByType(reportType, pageable);
        } else if (status != null) {
            reports = reportService.getReportsByStatus(status, pageable);
        } else {
            reports = reportService.getReports(pageable);
        }

        return ResponseEntity.ok(mapper.toPageResponse(reports.map(mapper::toResponse)));
    }

    @PutMapping("/{id}/submit")
    @Operation(summary = "Submit regulatory report", description = "Submits a regulatory report to the authorities")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report submitted successfully"),
            @ApiResponse(responseCode = "404", description = "Report not found"),
            @ApiResponse(responseCode = "400", description = "Report cannot be submitted in current status")
    })
    public ResponseEntity<RegulatoryReportResponse> submitReport(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitReportRequest request
    ) {
        RegulatoryReport report = reportService.submitReport(
                id,
                request.submittedBy(),
                request.regulatoryReference()
        );
        return ResponseEntity.ok(mapper.toResponse(report));
    }
}

