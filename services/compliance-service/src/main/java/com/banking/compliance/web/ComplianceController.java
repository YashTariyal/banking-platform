package com.banking.compliance.web;

import com.banking.compliance.domain.ComplianceRecord;
import com.banking.compliance.domain.ComplianceRecordType;
import com.banking.compliance.domain.ComplianceStatus;
import com.banking.compliance.service.ComplianceService;
import com.banking.compliance.web.dto.ComplianceRecordResponse;
import com.banking.compliance.web.dto.PageResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compliance/records")
@Tag(name = "Compliance Records", description = "Compliance record management operations")
public class ComplianceController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ComplianceService complianceService;
    private final ComplianceMapper mapper;

    public ComplianceController(ComplianceService complianceService, ComplianceMapper mapper) {
        this.complianceService = complianceService;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get compliance record by ID", description = "Retrieves a specific compliance record by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compliance record found"),
            @ApiResponse(responseCode = "404", description = "Compliance record not found")
    })
    public ResponseEntity<ComplianceRecordResponse> getComplianceRecord(
            @Parameter(description = "Compliance record unique identifier", required = true)
            @PathVariable UUID id
    ) {
        ComplianceRecord record = complianceService.getComplianceRecord(id);
        return ResponseEntity.ok(mapper.toResponse(record));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get compliance records by customer", description = "Retrieves paginated compliance records for a specific customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compliance records retrieved successfully")
    })
    public ResponseEntity<PageResponse<ComplianceRecordResponse>> getComplianceRecordsByCustomer(
            @Parameter(description = "Customer unique identifier", required = true)
            @PathVariable UUID customerId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        int pageSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<ComplianceRecord> records = complianceService.getComplianceRecordsByCustomer(customerId, pageable);
        return ResponseEntity.ok(mapper.toPageResponse(records.map(mapper::toResponse)));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get compliance records by account", description = "Retrieves paginated compliance records for a specific account")
    public ResponseEntity<PageResponse<ComplianceRecordResponse>> getComplianceRecordsByAccount(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int pageSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<ComplianceRecord> records = complianceService.getComplianceRecordsByAccount(accountId, pageable);
        return ResponseEntity.ok(mapper.toPageResponse(records.map(mapper::toResponse)));
    }

    @GetMapping
    @Operation(summary = "List compliance records", description = "Retrieves paginated compliance records with optional filters")
    public ResponseEntity<PageResponse<ComplianceRecordResponse>> listComplianceRecords(
            @RequestParam(required = false) ComplianceRecordType recordType,
            @RequestParam(required = false) ComplianceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int pageSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<ComplianceRecord> records;

        if (recordType != null && status != null) {
            Page<ComplianceRecord> statusRecords = complianceService.getComplianceRecordsByStatus(status, pageable);
            List<ComplianceRecord> filtered = statusRecords.getContent().stream()
                    .filter(r -> r.getRecordType() == recordType)
                    .toList();
            records = new PageImpl<>(filtered, pageable, filtered.size());
        } else if (recordType != null) {
            records = complianceService.getComplianceRecordsByType(recordType, pageable);
        } else if (status != null) {
            records = complianceService.getComplianceRecordsByStatus(status, pageable);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return ResponseEntity.ok(mapper.toPageResponse(records.map(mapper::toResponse)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update compliance record status", description = "Updates the status of a compliance record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Compliance record not found")
    })
    public ResponseEntity<ComplianceRecordResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        ComplianceRecord record = complianceService.updateComplianceRecordStatus(id, request.status());
        return ResponseEntity.ok(mapper.toResponse(record));
    }

    public record UpdateStatusRequest(ComplianceStatus status) {
    }
}

