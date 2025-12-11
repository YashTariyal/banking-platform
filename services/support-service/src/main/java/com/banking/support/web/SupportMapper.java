package com.banking.support.web;

import com.banking.support.domain.ManualOverride;
import com.banking.support.domain.SupportCase;
import com.banking.support.web.dto.ManualOverrideResponse;
import com.banking.support.web.dto.SupportCaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class SupportMapper {

    public SupportCaseResponse toResponse(SupportCase supportCase) {
        return new SupportCaseResponse(
                supportCase.getId(),
                supportCase.getCaseNumber(),
                supportCase.getCaseType(),
                supportCase.getPriority(),
                supportCase.getStatus(),
                supportCase.getCustomerId(),
                supportCase.getAccountId(),
                supportCase.getTitle(),
                supportCase.getDescription(),
                supportCase.getAssignedTo(),
                supportCase.getCreatedBy(),
                supportCase.getResolvedBy(),
                supportCase.getResolvedAt(),
                supportCase.getResolutionNotes(),
                supportCase.getDueDate(),
                supportCase.getCreatedAt(),
                supportCase.getUpdatedAt()
        );
    }

    public ManualOverrideResponse toResponse(ManualOverride override) {
        return new ManualOverrideResponse(
                override.getId(),
                override.getOverrideType(),
                override.getStatus(),
                override.getCustomerId(),
                override.getAccountId(),
                override.getEntityId(),
                override.getRequestedBy(),
                override.getApprovedBy(),
                override.getRejectedBy(),
                override.getReason(),
                override.getOverrideValue(),
                override.getAmount(),
                override.getCurrency(),
                override.getExpiresAt(),
                override.getApprovedAt(),
                override.getRejectedAt(),
                override.getRejectionReason(),
                override.getCreatedAt(),
                override.getUpdatedAt()
        );
    }

    public <T> com.banking.support.web.dto.PageResponse<T> toPageResponse(Page<T> page) {
        return new com.banking.support.web.dto.PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}

