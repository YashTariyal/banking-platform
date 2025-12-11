package com.banking.support.service;

import com.banking.support.domain.CasePriority;
import com.banking.support.domain.CaseStatus;
import com.banking.support.domain.CaseType;
import com.banking.support.domain.SupportCase;
import com.banking.support.messaging.SupportEventPublisher;
import com.banking.support.repository.SupportCaseRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupportCaseService {

    private final SupportCaseRepository caseRepository;
    private final CaseNumberGenerator caseNumberGenerator;
    private final SupportEventPublisher eventPublisher;
    private final Clock clock;

    public SupportCaseService(
            SupportCaseRepository caseRepository,
            CaseNumberGenerator caseNumberGenerator,
            SupportEventPublisher eventPublisher,
            Clock clock
    ) {
        this.caseRepository = caseRepository;
        this.caseNumberGenerator = caseNumberGenerator;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public SupportCase createCase(
            CaseType caseType,
            CasePriority priority,
            UUID customerId,
            UUID accountId,
            String title,
            String description,
            UUID createdBy,
            Instant dueDate
    ) {
        SupportCase supportCase = new SupportCase();
        supportCase.setCaseNumber(caseNumberGenerator.generateCaseNumber());
        supportCase.setCaseType(caseType);
        supportCase.setPriority(priority);
        supportCase.setCustomerId(customerId);
        supportCase.setAccountId(accountId);
        supportCase.setTitle(title);
        supportCase.setDescription(description);
        supportCase.setCreatedBy(createdBy);
        supportCase.setDueDate(dueDate);
        supportCase.setStatus(CaseStatus.OPEN);

        SupportCase saved = caseRepository.save(supportCase);
        eventPublisher.publishCaseCreated(saved);

        return saved;
    }

    public SupportCase getCase(UUID caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Support case not found: " + caseId
                ));
    }

    public SupportCase getCaseByNumber(String caseNumber) {
        return caseRepository.findByCaseNumber(caseNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Support case not found: " + caseNumber
                ));
    }

    @Transactional
    public SupportCase assignCase(UUID caseId, UUID assignedTo) {
        SupportCase supportCase = getCase(caseId);
        supportCase.setAssignedTo(assignedTo);
        supportCase.setStatus(CaseStatus.ASSIGNED);

        SupportCase updated = caseRepository.save(supportCase);
        eventPublisher.publishCaseUpdated(updated);

        return updated;
    }

    @Transactional
    public SupportCase updateStatus(UUID caseId, CaseStatus status, UUID updatedBy) {
        SupportCase supportCase = getCase(caseId);
        supportCase.setStatus(status);

        if (status == CaseStatus.RESOLVED || status == CaseStatus.CLOSED) {
            supportCase.setResolvedBy(updatedBy);
            supportCase.setResolvedAt(Instant.now(clock));
        }

        SupportCase updated = caseRepository.save(supportCase);
        eventPublisher.publishCaseUpdated(updated);

        return updated;
    }

    @Transactional
    public SupportCase resolveCase(UUID caseId, String resolutionNotes, UUID resolvedBy) {
        SupportCase supportCase = getCase(caseId);
        supportCase.setStatus(CaseStatus.RESOLVED);
        supportCase.setResolutionNotes(resolutionNotes);
        supportCase.setResolvedBy(resolvedBy);
        supportCase.setResolvedAt(Instant.now(clock));

        SupportCase updated = caseRepository.save(supportCase);
        eventPublisher.publishCaseResolved(updated);

        return updated;
    }

    public Page<SupportCase> getCasesByStatus(CaseStatus status, Pageable pageable) {
        return caseRepository.findByStatus(status, pageable);
    }

    public Page<SupportCase> getCasesByPriority(CasePriority priority, Pageable pageable) {
        return caseRepository.findByPriority(priority, pageable);
    }

    public Page<SupportCase> getCasesByCustomer(UUID customerId, Pageable pageable) {
        return caseRepository.findByCustomerId(customerId, pageable);
    }

    public Page<SupportCase> getCasesByAssignedTo(UUID assignedTo, Pageable pageable) {
        return caseRepository.findByAssignedTo(assignedTo, pageable);
    }

    public Page<SupportCase> getCasesByType(CaseType caseType, Pageable pageable) {
        return caseRepository.findByCaseType(caseType, pageable);
    }

    public Page<SupportCase> getOpenCases(Pageable pageable) {
        return caseRepository.findByStatus(CaseStatus.OPEN, pageable);
    }
}

