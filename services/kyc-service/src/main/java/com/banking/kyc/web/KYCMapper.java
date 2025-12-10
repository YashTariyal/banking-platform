package com.banking.kyc.web;

import com.banking.kyc.domain.Document;
import com.banking.kyc.domain.KYCCase;
import com.banking.kyc.domain.ScreeningResult;
import com.banking.kyc.web.dto.DocumentResponse;
import com.banking.kyc.web.dto.KYCCaseResponse;
import com.banking.kyc.web.dto.PageResponse;
import com.banking.kyc.web.dto.ScreeningResultResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class KYCMapper {

    public KYCCaseResponse toResponse(KYCCase kycCase) {
        return new KYCCaseResponse(
                kycCase.getId(),
                kycCase.getCustomerId(),
                kycCase.getStatus(),
                kycCase.getRiskLevel(),
                kycCase.getCaseType(),
                kycCase.getAssignedTo(),
                kycCase.getReviewNotes(),
                kycCase.getScreeningCompleted(),
                kycCase.getDocumentVerificationCompleted(),
                kycCase.getApprovedAt(),
                kycCase.getRejectedAt(),
                kycCase.getRejectionReason(),
                kycCase.getDueDate(),
                kycCase.getCompletedAt(),
                kycCase.getCreatedAt(),
                kycCase.getUpdatedAt()
        );
    }

    public DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getKycCaseId(),
                document.getDocumentType(),
                document.getFileName(),
                document.getFilePath(),
                document.getFileSize(),
                document.getMimeType(),
                document.getVerificationStatus(),
                document.getVerifiedAt(),
                document.getVerifiedBy(),
                document.getVerificationNotes(),
                document.getExpiryDate(),
                document.getDocumentNumber(),
                document.getIssuingCountry(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    public ScreeningResultResponse toResponse(ScreeningResult result) {
        return new ScreeningResultResponse(
                result.getId(),
                result.getKycCaseId(),
                result.getCustomerId(),
                result.getScreeningType(),
                result.getResult(),
                result.getMatchScore(),
                result.getMatchedName(),
                result.getMatchedList(),
                result.getMatchDetails(),
                result.getScreeningProvider(),
                result.getScreeningReference(),
                result.getReviewedAt(),
                result.getReviewedBy(),
                result.getReviewNotes(),
                result.getCreatedAt(),
                result.getUpdatedAt()
        );
    }

    public <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}

