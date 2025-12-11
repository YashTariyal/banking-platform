package com.banking.risk.web;

import com.banking.risk.domain.RiskAssessment;
import com.banking.risk.domain.RiskLevel;
import com.banking.risk.domain.RiskType;
import com.banking.risk.service.RiskAssessmentService;
import com.banking.risk.service.RiskScoringService;
import com.banking.risk.web.dto.AssessRiskRequest;
import com.banking.risk.web.dto.PageResponse;
import com.banking.risk.web.dto.RiskAssessmentResponse;
import java.util.List;
import java.util.stream.Collectors;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk/assessments")
public class RiskAssessmentController {

    private final RiskAssessmentService assessmentService;
    private final RiskMapper mapper;

    public RiskAssessmentController(RiskAssessmentService assessmentService, RiskMapper mapper) {
        this.assessmentService = assessmentService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RiskAssessmentResponse assessRisk(@Valid @RequestBody AssessRiskRequest request) {
        RiskScoringService.RiskScoringContext context = new RiskScoringService.RiskScoringContext();
        // In a real system, populate context from historical data

        RiskAssessment assessment = assessmentService.assessRisk(
                request.riskType(),
                request.entityId(),
                request.customerId(),
                request.accountId(),
                request.amount(),
                request.currency(),
                request.description(),
                context
        );
        return mapper.toResponse(assessment);
    }

    @GetMapping("/{id}")
    public RiskAssessmentResponse getAssessment(@PathVariable UUID id) {
        RiskAssessment assessment = assessmentService.getAssessment(id);
        return mapper.toResponse(assessment);
    }

    @GetMapping
    public PageResponse<RiskAssessmentResponse> getAssessments(
            @RequestParam(required = false) RiskType riskType,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID accountId,
            @ParameterObject @PageableDefault(size = 20, sort = "assessedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (riskType != null) {
            return mapper.toPageResponse(
                    assessmentService.getAssessmentsByType(riskType, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (riskLevel != null) {
            return mapper.toPageResponse(
                    assessmentService.getAssessmentsByRiskLevel(riskLevel, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (customerId != null) {
            return mapper.toPageResponse(
                    assessmentService.getAssessmentsByCustomer(customerId, pageable)
                            .map(mapper::toResponse)
            );
        }
        if (accountId != null) {
            return mapper.toPageResponse(
                    assessmentService.getAssessmentsByAccount(accountId, pageable)
                            .map(mapper::toResponse)
            );
        }
        // Default: return high risk assessments
        return mapper.toPageResponse(
                assessmentService.getAssessmentsByRiskLevel(RiskLevel.HIGH, pageable)
                        .map(mapper::toResponse)
        );
    }

    @GetMapping("/entity/{riskType}/{entityId}")
    public PageResponse<RiskAssessmentResponse> getAssessmentsByEntity(
            @PathVariable RiskType riskType,
            @PathVariable UUID entityId
    ) {
        List<RiskAssessmentResponse> responses = assessmentService.getAssessmentsByEntity(riskType, entityId)
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
}

