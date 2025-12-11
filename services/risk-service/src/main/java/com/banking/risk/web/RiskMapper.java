package com.banking.risk.web;

import com.banking.risk.domain.RiskAlert;
import com.banking.risk.domain.RiskAssessment;
import com.banking.risk.web.dto.RiskAlertResponse;
import com.banking.risk.web.dto.RiskAssessmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class RiskMapper {

    public RiskAssessmentResponse toResponse(RiskAssessment assessment) {
        return new RiskAssessmentResponse(
                assessment.getId(),
                assessment.getRiskType(),
                assessment.getEntityId(),
                assessment.getRiskLevel(),
                assessment.getRiskScore(),
                assessment.getCustomerId(),
                assessment.getAccountId(),
                assessment.getAmount(),
                assessment.getCurrency(),
                assessment.getRiskFactors(),
                assessment.getDescription(),
                assessment.getAssessedAt(),
                assessment.getCreatedAt(),
                assessment.getUpdatedAt()
        );
    }

    public RiskAlertResponse toResponse(RiskAlert alert) {
        return new RiskAlertResponse(
                alert.getId(),
                alert.getRiskAssessmentId(),
                alert.getStatus(),
                alert.getRiskLevel(),
                alert.getRiskScore(),
                alert.getCustomerId(),
                alert.getAccountId(),
                alert.getTitle(),
                alert.getDescription(),
                alert.getReviewedBy(),
                alert.getReviewedAt(),
                alert.getResolutionNotes(),
                alert.getCreatedAt(),
                alert.getUpdatedAt()
        );
    }

    public <T> com.banking.risk.web.dto.PageResponse<T> toPageResponse(Page<T> page) {
        return new com.banking.risk.web.dto.PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}

