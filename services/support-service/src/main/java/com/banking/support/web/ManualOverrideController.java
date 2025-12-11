package com.banking.support.web;

import com.banking.support.domain.ManualOverride;
import com.banking.support.domain.OverrideStatus;
import com.banking.support.domain.OverrideType;
import com.banking.support.service.ManualOverrideService;
import com.banking.support.web.dto.CreateOverrideRequest;
import com.banking.support.web.dto.ManualOverrideResponse;
import com.banking.support.web.dto.PageResponse;
import com.banking.support.web.dto.RejectOverrideRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support/overrides")
public class ManualOverrideController {

    private final ManualOverrideService overrideService;
    private final SupportMapper mapper;

    public ManualOverrideController(ManualOverrideService overrideService, SupportMapper mapper) {
        this.overrideService = overrideService;
        this.mapper = mapper;
    }

    @PostMapping
    public ManualOverrideResponse createOverride(
            @Valid @RequestBody CreateOverrideRequest request,
            @RequestParam UUID requestedBy
    ) {
        ManualOverride override = overrideService.createOverride(
                request.overrideType(),
                request.customerId(),
                request.accountId(),
                request.entityId(),
                request.reason(),
                request.overrideValue(),
                request.amount(),
                request.currency(),
                request.expiresAt(),
                requestedBy
        );
        return mapper.toResponse(override);
    }

    @GetMapping("/{id}")
    public ManualOverrideResponse getOverride(@PathVariable UUID id) {
        ManualOverride override = overrideService.getOverride(id);
        return mapper.toResponse(override);
    }

    @GetMapping
    public PageResponse<ManualOverrideResponse> getOverrides(
            @RequestParam(required = false) OverrideStatus status,
            @RequestParam(required = false) OverrideType overrideType,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID accountId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (status != null) {
            return mapper.toPageResponse(
                    overrideService.getOverridesByStatus(status, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (overrideType != null) {
            return mapper.toPageResponse(
                    overrideService.getOverridesByType(overrideType, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (customerId != null) {
            return mapper.toPageResponse(
                    overrideService.getOverridesByCustomer(customerId, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (accountId != null) {
            return mapper.toPageResponse(
                    overrideService.getOverridesByAccount(accountId, pageable)
                            .map(mapper::toResponse)
            );
        }
        // Default: return pending overrides
        return mapper.toPageResponse(
                overrideService.getOverridesByStatus(OverrideStatus.PENDING, pageable)
                        .map(mapper::toResponse)
        );
    }

    @GetMapping("/active/{customerId}/{overrideType}")
    public PageResponse<ManualOverrideResponse> getActiveOverrides(
            @PathVariable UUID customerId,
            @PathVariable OverrideType overrideType
    ) {
        List<ManualOverrideResponse> responses = overrideService.getActiveOverrides(customerId, overrideType)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                responses,
                0,
                responses.size(),
                responses.size(),
                1
        );
    }

    @PutMapping("/{id}/approve")
    public ManualOverrideResponse approveOverride(
            @PathVariable UUID id,
            @RequestParam UUID approvedBy
    ) {
        ManualOverride override = overrideService.approveOverride(id, approvedBy);
        return mapper.toResponse(override);
    }

    @PutMapping("/{id}/reject")
    public ManualOverrideResponse rejectOverride(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOverrideRequest request,
            @RequestParam UUID rejectedBy
    ) {
        ManualOverride override = overrideService.rejectOverride(id, request.rejectionReason(), rejectedBy);
        return mapper.toResponse(override);
    }

    @PutMapping("/{id}/revoke")
    public ManualOverrideResponse revokeOverride(
            @PathVariable UUID id,
            @RequestParam UUID revokedBy
    ) {
        ManualOverride override = overrideService.revokeOverride(id, revokedBy);
        return mapper.toResponse(override);
    }
}

