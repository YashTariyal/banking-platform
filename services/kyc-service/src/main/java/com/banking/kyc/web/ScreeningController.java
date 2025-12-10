package com.banking.kyc.web;

import com.banking.kyc.domain.ScreeningResult;
import com.banking.kyc.domain.ScreeningResultStatus;
import com.banking.kyc.domain.ScreeningType;
import com.banking.kyc.service.ScreeningService;
import com.banking.kyc.web.dto.CreateScreeningRequest;
import com.banking.kyc.web.dto.ScreeningResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kyc/screening")
@Tag(name = "Screening", description = "Sanctions and PEP screening operations")
public class ScreeningController {

    private final ScreeningService screeningService;
    private final KYCMapper mapper;

    public ScreeningController(ScreeningService screeningService, KYCMapper mapper) {
        this.screeningService = screeningService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Perform screening",
            description = "Performs sanctions, PEP, or other screening for a KYC case. Automatically updates case status when all screenings are completed."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Screening performed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "KYC case not found")
    })
    public ResponseEntity<ScreeningResultResponse> performScreening(@Valid @RequestBody CreateScreeningRequest request) {
        ScreeningResult result = screeningService.performScreening(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(result));
    }

    @GetMapping("/case/{kycCaseId}")
    @Operation(
            summary = "Get screening results for a KYC case",
            description = "Retrieves all screening results for a specific KYC case"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Screening results retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "KYC case not found")
    })
    public ResponseEntity<List<ScreeningResultResponse>> getScreeningResults(
            @Parameter(description = "KYC case unique identifier", required = true)
            @PathVariable UUID kycCaseId
    ) {
        List<ScreeningResult> results = screeningService.getScreeningResults(kycCaseId);
        return ResponseEntity.ok(results.stream().map(mapper::toResponse).toList());
    }

    @PutMapping("/{id}/review")
    @Operation(
            summary = "Review a screening result",
            description = "Reviews a screening result and marks it as false positive or confirmed match"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Screening result reviewed successfully"),
            @ApiResponse(responseCode = "404", description = "Screening result not found")
    })
    public ResponseEntity<ScreeningResultResponse> reviewScreeningResult(
            @PathVariable UUID id,
            @RequestBody ReviewScreeningRequest request
    ) {
        ScreeningResult result = screeningService.reviewScreeningResult(
                id, request.reviewedBy(), request.reviewNotes(), request.finalResult()
        );
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    public record ReviewScreeningRequest(UUID reviewedBy, String reviewNotes, ScreeningResultStatus finalResult) {
    }
}

