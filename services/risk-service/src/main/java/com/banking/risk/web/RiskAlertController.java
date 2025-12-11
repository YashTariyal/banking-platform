package com.banking.risk.web;

import com.banking.risk.domain.AlertStatus;
import com.banking.risk.domain.RiskAlert;
import com.banking.risk.domain.RiskLevel;
import com.banking.risk.service.RiskAlertService;
import com.banking.risk.web.dto.PageResponse;
import com.banking.risk.web.dto.RiskAlertResponse;
import com.banking.risk.web.dto.UpdateAlertStatusRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk/alerts")
public class RiskAlertController {

    private final RiskAlertService alertService;
    private final RiskMapper mapper;

    public RiskAlertController(RiskAlertService alertService, RiskMapper mapper) {
        this.alertService = alertService;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    public RiskAlertResponse getAlert(@PathVariable UUID id) {
        RiskAlert alert = alertService.getAlert(id);
        return mapper.toResponse(alert);
    }

    @GetMapping
    public PageResponse<RiskAlertResponse> getAlerts(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID accountId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (status != null) {
            return mapper.toPageResponse(
                    alertService.getAlertsByStatus(status, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (riskLevel != null) {
            return mapper.toPageResponse(
                    alertService.getAlertsByRiskLevel(riskLevel, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (customerId != null) {
            return mapper.toPageResponse(
                    alertService.getAlertsByCustomer(customerId, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (accountId != null) {
            return mapper.toPageResponse(
                    alertService.getAlertsByAccount(accountId, pageable)
                            .map(mapper::toResponse)
            );
        }
        // Default: return open alerts
        return mapper.toPageResponse(
                alertService.getOpenAlerts(pageable)
                        .map(mapper::toResponse)
        );
    }

    @PutMapping("/{id}/status")
    public RiskAlertResponse updateAlertStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAlertStatusRequest request
    ) {
        RiskAlert alert = alertService.updateAlertStatus(
                id,
                request.status(),
                request.reviewedBy(),
                request.resolutionNotes()
        );
        return mapper.toResponse(alert);
    }
}

