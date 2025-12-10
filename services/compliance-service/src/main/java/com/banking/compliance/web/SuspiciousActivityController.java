package com.banking.compliance.web;

import com.banking.compliance.domain.Severity;
import com.banking.compliance.domain.SuspiciousActivity;
import com.banking.compliance.domain.SuspiciousActivityStatus;
import com.banking.compliance.domain.SuspiciousActivityType;
import com.banking.compliance.service.ComplianceService;
import com.banking.compliance.web.dto.PageResponse;
import com.banking.compliance.web.dto.SuspiciousActivityResponse;
import com.banking.compliance.web.dto.UpdateSuspiciousActivityRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compliance/suspicious-activities")
@Tag(name = "Suspicious Activities", description = "Suspicious activity management operations")
public class SuspiciousActivityController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ComplianceService complianceService;
    private final ComplianceMapper mapper;

    public SuspiciousActivityController(ComplianceService complianceService, ComplianceMapper mapper) {
        this.complianceService = complianceService;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get suspicious activity by ID", description = "Retrieves a specific suspicious activity by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Suspicious activity found"),
            @ApiResponse(responseCode = "404", description = "Suspicious activity not found")
    })
    public ResponseEntity<SuspiciousActivityResponse> getSuspiciousActivity(
            @Parameter(description = "Suspicious activity unique identifier", required = true)
            @PathVariable UUID id
    ) {
        SuspiciousActivity activity = complianceService.getSuspiciousActivity(id);
        return ResponseEntity.ok(mapper.toResponse(activity));
    }

    @GetMapping
    @Operation(summary = "List suspicious activities", description = "Retrieves paginated suspicious activities with optional filters")
    public ResponseEntity<PageResponse<SuspiciousActivityResponse>> listSuspiciousActivities(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) SuspiciousActivityStatus status,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) SuspiciousActivityType activityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int pageSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<SuspiciousActivity> activities;

        if (customerId != null) {
            activities = complianceService.getSuspiciousActivitiesByCustomer(customerId, pageable);
        } else if (status != null && severity != null) {
            Page<SuspiciousActivity> statusActivities = complianceService.getSuspiciousActivitiesByStatus(status, pageable);
            List<SuspiciousActivity> filtered = statusActivities.getContent().stream()
                    .filter(a -> a.getSeverity() == severity)
                    .toList();
            activities = new PageImpl<>(filtered, pageable, filtered.size());
        } else if (status != null) {
            activities = complianceService.getSuspiciousActivitiesByStatus(status, pageable);
        } else {
            activities = complianceService.getSuspiciousActivities(pageable);
        }

        return ResponseEntity.ok(mapper.toPageResponse(activities.map(mapper::toResponse)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update suspicious activity", description = "Updates the status and investigation details of a suspicious activity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Suspicious activity updated successfully"),
            @ApiResponse(responseCode = "404", description = "Suspicious activity not found")
    })
    public ResponseEntity<SuspiciousActivityResponse> updateSuspiciousActivity(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSuspiciousActivityRequest request
    ) {
        SuspiciousActivity activity = complianceService.updateSuspiciousActivityStatus(
                id,
                request.status(),
                request.investigatorId(),
                request.investigationNotes()
        );
        return ResponseEntity.ok(mapper.toResponse(activity));
    }
}

