package com.banking.kyc.web;

import com.banking.kyc.domain.KYCStatus;
import com.banking.kyc.domain.KYCCase;
import com.banking.kyc.service.KYCService;
import com.banking.kyc.web.dto.CreateKYCCaseRequest;
import com.banking.kyc.web.dto.KYCCaseResponse;
import com.banking.kyc.web.dto.PageResponse;
import com.banking.kyc.web.dto.UpdateKYCCaseRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
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
@RequestMapping("/api/kyc/cases")
@Tag(name = "KYC Cases", description = "KYC case management operations")
public class KYCController {

    private static final int MAX_PAGE_SIZE = 100;

    private final KYCService kycService;
    private final KYCMapper mapper;

    public KYCController(KYCService kycService, KYCMapper mapper) {
        this.kycService = kycService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new KYC case",
            description = "Creates a new KYC case for a customer. Automatically created when customer is registered."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "KYC case created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Active KYC case already exists")
    })
    public ResponseEntity<KYCCaseResponse> createKYCCase(@Valid @RequestBody CreateKYCCaseRequest request) {
        KYCCase kycCase = kycService.createKYCCase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(kycCase));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get KYC case by ID",
            description = "Retrieves detailed information about a specific KYC case"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC case found"),
            @ApiResponse(responseCode = "404", description = "KYC case not found")
    })
    public ResponseEntity<KYCCaseResponse> getKYCCase(
            @Parameter(description = "KYC case unique identifier", required = true)
            @PathVariable UUID id
    ) {
        KYCCase kycCase = kycService.getKYCCase(id);
        return ResponseEntity.ok(mapper.toResponse(kycCase));
    }

    @GetMapping
    @Operation(
            summary = "List KYC cases",
            description = "Retrieves a paginated list of KYC cases. Optionally filter by customer or status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC cases retrieved successfully")
    })
    public ResponseEntity<PageResponse<KYCCaseResponse>> listKYCCases(
            @Parameter(description = "Filter by customer ID (optional)")
            @RequestParam(required = false) UUID customerId,
            @Parameter(description = "Filter by status (optional)")
            @RequestParam(required = false) KYCStatus status,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        int pageSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<KYCCase> cases;

        if (customerId != null) {
            cases = kycService.listKYCCasesByCustomer(customerId, pageable);
        } else if (status != null) {
            cases = kycService.listKYCCasesByStatus(status, pageable);
        } else {
            cases = kycService.listKYCCases(pageable);
        }

        return ResponseEntity.ok(mapper.toPageResponse(cases.map(mapper::toResponse)));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update KYC case",
            description = "Updates KYC case information. Only provided fields will be updated."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC case updated successfully"),
            @ApiResponse(responseCode = "404", description = "KYC case not found")
    })
    public ResponseEntity<KYCCaseResponse> updateKYCCase(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateKYCCaseRequest request
    ) {
        KYCCase kycCase = kycService.updateKYCCase(id, request);
        return ResponseEntity.ok(mapper.toResponse(kycCase));
    }

    @PostMapping("/{id}/approve")
    @Operation(
            summary = "Approve KYC case",
            description = "Approves a KYC case. Requires screening and document verification to be completed."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC case approved successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot approve: requirements not met"),
            @ApiResponse(responseCode = "404", description = "KYC case not found")
    })
    public ResponseEntity<KYCCaseResponse> approveKYCCase(
            @PathVariable UUID id,
            @RequestBody ApproveKYCCaseRequest request
    ) {
        KYCCase kycCase = kycService.approveKYCCase(id, request.approvedBy(), request.notes());
        return ResponseEntity.ok(mapper.toResponse(kycCase));
    }

    @PostMapping("/{id}/reject")
    @Operation(
            summary = "Reject KYC case",
            description = "Rejects a KYC case with a reason"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC case rejected successfully"),
            @ApiResponse(responseCode = "404", description = "KYC case not found")
    })
    public ResponseEntity<KYCCaseResponse> rejectKYCCase(
            @PathVariable UUID id,
            @RequestBody RejectKYCCaseRequest request
    ) {
        KYCCase kycCase = kycService.rejectKYCCase(id, request.rejectedBy(), request.reason());
        return ResponseEntity.ok(mapper.toResponse(kycCase));
    }

    public record ApproveKYCCaseRequest(UUID approvedBy, String notes) {
    }

    public record RejectKYCCaseRequest(UUID rejectedBy, String reason) {
    }
}

