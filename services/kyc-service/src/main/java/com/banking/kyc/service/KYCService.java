package com.banking.kyc.service;

import com.banking.kyc.domain.KYCCase;
import com.banking.kyc.domain.KYCStatus;
import com.banking.kyc.domain.RiskLevel;
import com.banking.kyc.messaging.KYCEventPublisher;
import com.banking.kyc.repository.KYCCaseRepository;
import com.banking.kyc.web.dto.CreateKYCCaseRequest;
import com.banking.kyc.web.dto.UpdateKYCCaseRequest;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class KYCService {

    private static final int MAX_PAGE_SIZE = 100;

    private final KYCCaseRepository kycCaseRepository;
    private final KYCEventPublisher eventPublisher;

    public KYCService(KYCCaseRepository kycCaseRepository, KYCEventPublisher eventPublisher) {
        this.kycCaseRepository = kycCaseRepository;
        this.eventPublisher = eventPublisher;
    }

    public KYCCase createKYCCase(CreateKYCCaseRequest request) {
        // Check if there's already an active case for this customer
        List<KYCStatus> excludedStatuses = List.of(KYCStatus.APPROVED, KYCStatus.REJECTED);
        kycCaseRepository.findByCustomerIdAndStatusNotIn(request.customerId(), excludedStatuses)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Active KYC case already exists for customer: " + request.customerId());
                });

        KYCCase kycCase = new KYCCase();
        kycCase.setCustomerId(request.customerId());
        kycCase.setCaseType(request.caseType() != null ? request.caseType() : "ONBOARDING");
        kycCase.setStatus(KYCStatus.PENDING);
        kycCase.setRiskLevel(request.riskLevel() != null ? request.riskLevel() : RiskLevel.LOW);
        kycCase.setAssignedTo(request.assignedTo());
        kycCase.setDueDate(request.dueDate());

        KYCCase saved = kycCaseRepository.save(kycCase);
        eventPublisher.publishKYCCaseCreated(saved);
        return saved;
    }

    public KYCCase getKYCCase(UUID id) {
        return kycCaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("KYC case not found: " + id));
    }

    public Page<KYCCase> listKYCCases(Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable validPageable = PageRequest.of(pageable.getPageNumber(), pageSize);
        return kycCaseRepository.findAll(validPageable);
    }

    public Page<KYCCase> listKYCCasesByCustomer(UUID customerId, Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable validPageable = PageRequest.of(pageable.getPageNumber(), pageSize);
        return kycCaseRepository.findByCustomerId(customerId, validPageable);
    }

    public Page<KYCCase> listKYCCasesByStatus(KYCStatus status, Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable validPageable = PageRequest.of(pageable.getPageNumber(), pageSize);
        return kycCaseRepository.findByStatus(status, validPageable);
    }

    public KYCCase updateKYCCase(UUID id, UpdateKYCCaseRequest request) {
        KYCCase kycCase = getKYCCase(id);

        if (request.status() != null) {
            kycCase.setStatus(request.status());
        }
        if (request.riskLevel() != null) {
            kycCase.setRiskLevel(request.riskLevel());
        }
        if (request.assignedTo() != null) {
            kycCase.setAssignedTo(request.assignedTo());
        }
        if (request.reviewNotes() != null) {
            kycCase.setReviewNotes(request.reviewNotes());
        }
        if (request.dueDate() != null) {
            kycCase.setDueDate(request.dueDate());
        }

        KYCCase updated = kycCaseRepository.save(kycCase);
        eventPublisher.publishKYCCaseUpdated(updated);
        return updated;
    }

    public KYCCase approveKYCCase(UUID id, UUID approvedBy, String notes) {
        KYCCase kycCase = getKYCCase(id);

        if (!kycCase.getScreeningCompleted() || !kycCase.getDocumentVerificationCompleted()) {
            throw new IllegalStateException("Cannot approve KYC case: screening or document verification not completed");
        }

        kycCase.setStatus(KYCStatus.APPROVED);
        kycCase.setApprovedAt(Instant.now());
        kycCase.setCompletedAt(Instant.now());
        kycCase.setReviewNotes(notes);

        KYCCase approved = kycCaseRepository.save(kycCase);
        eventPublisher.publishKYCCaseApproved(approved);
        return approved;
    }

    public KYCCase rejectKYCCase(UUID id, UUID rejectedBy, String reason) {
        KYCCase kycCase = getKYCCase(id);

        kycCase.setStatus(KYCStatus.REJECTED);
        kycCase.setRejectedAt(Instant.now());
        kycCase.setCompletedAt(Instant.now());
        kycCase.setRejectionReason(reason);

        KYCCase rejected = kycCaseRepository.save(kycCase);
        eventPublisher.publishKYCCaseRejected(rejected);
        return rejected;
    }

    public void markScreeningCompleted(UUID kycCaseId) {
        KYCCase kycCase = getKYCCase(kycCaseId);
        kycCase.setScreeningCompleted(true);
        if (kycCase.getDocumentVerificationCompleted()) {
            kycCase.setStatus(KYCStatus.UNDER_REVIEW);
        } else {
            kycCase.setStatus(KYCStatus.DOCUMENT_REVIEW);
        }
        kycCaseRepository.save(kycCase);
    }

    public void markDocumentVerificationCompleted(UUID kycCaseId) {
        KYCCase kycCase = getKYCCase(kycCaseId);
        kycCase.setDocumentVerificationCompleted(true);
        if (kycCase.getScreeningCompleted()) {
            kycCase.setStatus(KYCStatus.UNDER_REVIEW);
        } else {
            kycCase.setStatus(KYCStatus.SCREENING);
        }
        kycCaseRepository.save(kycCase);
    }

    public List<KYCCase> getOverdueCases() {
        return kycCaseRepository.findOverdueCases(KYCStatus.PENDING, Instant.now());
    }
}

