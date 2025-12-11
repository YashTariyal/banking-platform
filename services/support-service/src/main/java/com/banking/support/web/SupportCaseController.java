package com.banking.support.web;

import com.banking.support.domain.CasePriority;
import com.banking.support.domain.CaseStatus;
import com.banking.support.domain.CaseType;
import com.banking.support.domain.SupportCase;
import com.banking.support.service.SupportCaseService;
import com.banking.support.web.dto.CreateCaseRequest;
import com.banking.support.web.dto.PageResponse;
import com.banking.support.web.dto.ResolveCaseRequest;
import com.banking.support.web.dto.SupportCaseResponse;
import com.banking.support.web.dto.UpdateCaseStatusRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/support/cases")
public class SupportCaseController {

    private final SupportCaseService caseService;
    private final SupportMapper mapper;

    public SupportCaseController(SupportCaseService caseService, SupportMapper mapper) {
        this.caseService = caseService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupportCaseResponse createCase(
            @Valid @RequestBody CreateCaseRequest request,
            @RequestParam UUID createdBy
    ) {
        SupportCase supportCase = caseService.createCase(
                request.caseType(),
                request.priority(),
                request.customerId(),
                request.accountId(),
                request.title(),
                request.description(),
                createdBy,
                request.dueDate()
        );
        return mapper.toResponse(supportCase);
    }

    @GetMapping("/{id}")
    public SupportCaseResponse getCase(@PathVariable UUID id) {
        SupportCase supportCase = caseService.getCase(id);
        return mapper.toResponse(supportCase);
    }

    @GetMapping("/number/{caseNumber}")
    public SupportCaseResponse getCaseByNumber(@PathVariable String caseNumber) {
        SupportCase supportCase = caseService.getCaseByNumber(caseNumber);
        return mapper.toResponse(supportCase);
    }

    @GetMapping
    public PageResponse<SupportCaseResponse> getCases(
            @RequestParam(required = false) CaseStatus status,
            @RequestParam(required = false) CasePriority priority,
            @RequestParam(required = false) CaseType caseType,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID assignedTo,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (status != null) {
            return mapper.toPageResponse(
                    caseService.getCasesByStatus(status, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (priority != null) {
            return mapper.toPageResponse(
                    caseService.getCasesByPriority(priority, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (caseType != null) {
            return mapper.toPageResponse(
                    caseService.getCasesByType(caseType, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (customerId != null) {
            return mapper.toPageResponse(
                    caseService.getCasesByCustomer(customerId, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (assignedTo != null) {
            return mapper.toPageResponse(
                    caseService.getCasesByAssignedTo(assignedTo, pageable)
                            .map(mapper::toResponse)
            );
        }
        // Default: return open cases
        return mapper.toPageResponse(
                caseService.getOpenCases(pageable)
                        .map(mapper::toResponse)
        );
    }

    @PutMapping("/{id}/assign")
    public SupportCaseResponse assignCase(
            @PathVariable UUID id,
            @RequestParam UUID assignedTo
    ) {
        SupportCase supportCase = caseService.assignCase(id, assignedTo);
        return mapper.toResponse(supportCase);
    }

    @PutMapping("/{id}/status")
    public SupportCaseResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCaseStatusRequest request,
            @RequestParam UUID updatedBy
    ) {
        SupportCase supportCase = caseService.updateStatus(id, request.status(), updatedBy);
        return mapper.toResponse(supportCase);
    }

    @PutMapping("/{id}/resolve")
    public SupportCaseResponse resolveCase(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveCaseRequest request,
            @RequestParam UUID resolvedBy
    ) {
        SupportCase supportCase = caseService.resolveCase(id, request.resolutionNotes(), resolvedBy);
        return mapper.toResponse(supportCase);
    }
}

