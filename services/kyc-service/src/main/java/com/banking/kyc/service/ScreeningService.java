package com.banking.kyc.service;

import com.banking.kyc.domain.ScreeningResult;
import com.banking.kyc.domain.ScreeningResultStatus;
import com.banking.kyc.domain.ScreeningType;
import com.banking.kyc.repository.KYCCaseRepository;
import com.banking.kyc.repository.ScreeningResultRepository;
import com.banking.kyc.web.dto.CreateScreeningRequest;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ScreeningService {

    private final ScreeningResultRepository screeningResultRepository;
    private final KYCCaseRepository kycCaseRepository;
    private final KYCService kycService;

    public ScreeningService(
            ScreeningResultRepository screeningResultRepository,
            KYCCaseRepository kycCaseRepository,
            KYCService kycService
    ) {
        this.screeningResultRepository = screeningResultRepository;
        this.kycCaseRepository = kycCaseRepository;
        this.kycService = kycService;
    }

    public ScreeningResult performScreening(CreateScreeningRequest request) {
        // Validate KYC case exists
        var kycCase = kycCaseRepository.findById(request.kycCaseId())
                .orElseThrow(() -> new IllegalArgumentException("KYC case not found: " + request.kycCaseId()));

        // In real implementation, this would call an external screening provider
        // For now, simulate screening result
        ScreeningResult result = new ScreeningResult();
        result.setKycCaseId(request.kycCaseId());
        result.setCustomerId(request.customerId());
        result.setScreeningType(request.screeningType());
        result.setScreeningProvider(request.screeningProvider() != null ? request.screeningProvider() : "INTERNAL");
        result.setScreeningReference(request.screeningReference());

        // Simulate screening logic
        ScreeningResultStatus screeningResult = determineScreeningResult(request);
        result.setResult(screeningResult);

        if (screeningResult == ScreeningResultStatus.MATCH) {
            result.setMatchScore(request.matchScore() != null ? request.matchScore() : 85);
            result.setMatchedName(request.matchedName());
            result.setMatchedList(request.matchedList());
            result.setMatchDetails(request.matchDetails());
        } else {
            result.setMatchScore(0);
        }

        ScreeningResult saved = screeningResultRepository.save(result);

        // Check if all required screenings are completed
        checkScreeningCompletion(request.kycCaseId());

        return saved;
    }

    public List<ScreeningResult> getScreeningResults(UUID kycCaseId) {
        kycCaseRepository.findById(kycCaseId)
                .orElseThrow(() -> new IllegalArgumentException("KYC case not found: " + kycCaseId));
        return screeningResultRepository.findByKycCaseId(kycCaseId);
    }

    public ScreeningResult reviewScreeningResult(UUID screeningResultId, UUID reviewedBy, String reviewNotes, ScreeningResultStatus finalResult) {
        ScreeningResult result = screeningResultRepository.findById(screeningResultId)
                .orElseThrow(() -> new IllegalArgumentException("Screening result not found: " + screeningResultId));

        result.setReviewedAt(java.time.Instant.now());
        result.setReviewedBy(reviewedBy);
        result.setReviewNotes(reviewNotes);
        result.setResult(finalResult);

        return screeningResultRepository.save(result);
    }

    private ScreeningResultStatus determineScreeningResult(CreateScreeningRequest request) {
        // In real implementation, this would call external screening API
        // For now, return CLEAR by default (can be overridden in request)
        return request.result() != null ? request.result() : ScreeningResultStatus.CLEAR;
    }

    private void checkScreeningCompletion(UUID kycCaseId) {
        // Check if all required screenings are completed
        // For simplicity, assume SANCTIONS and PEP are required
        List<ScreeningType> requiredTypes = List.of(ScreeningType.SANCTIONS, ScreeningType.PEP);
        boolean allCompleted = true;

        for (ScreeningType type : requiredTypes) {
            List<ScreeningResult> results = screeningResultRepository.findByKycCaseIdAndScreeningType(kycCaseId, type);
            if (results.isEmpty()) {
                allCompleted = false;
                break;
            }
        }

        if (allCompleted) {
            kycService.markScreeningCompleted(kycCaseId);
        }
    }
}

