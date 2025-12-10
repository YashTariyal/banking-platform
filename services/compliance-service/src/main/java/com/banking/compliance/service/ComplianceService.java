package com.banking.compliance.service;

import com.banking.compliance.domain.ComplianceRecord;
import com.banking.compliance.domain.ComplianceRecordType;
import com.banking.compliance.domain.ComplianceStatus;
import com.banking.compliance.domain.SuspiciousActivity;
import com.banking.compliance.domain.SuspiciousActivityStatus;
import com.banking.compliance.repository.ComplianceRecordRepository;
import com.banking.compliance.repository.SuspiciousActivityRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplianceService {

    private final ComplianceRecordRepository complianceRecordRepository;
    private final SuspiciousActivityRepository suspiciousActivityRepository;

    public ComplianceService(
            ComplianceRecordRepository complianceRecordRepository,
            SuspiciousActivityRepository suspiciousActivityRepository
    ) {
        this.complianceRecordRepository = complianceRecordRepository;
        this.suspiciousActivityRepository = suspiciousActivityRepository;
    }

    public Page<ComplianceRecord> getComplianceRecordsByCustomer(UUID customerId, Pageable pageable) {
        return complianceRecordRepository.findByCustomerId(customerId, pageable);
    }

    public Page<ComplianceRecord> getComplianceRecordsByAccount(UUID accountId, Pageable pageable) {
        return complianceRecordRepository.findByAccountId(accountId, pageable);
    }

    public Page<ComplianceRecord> getComplianceRecordsByType(ComplianceRecordType recordType, Pageable pageable) {
        return complianceRecordRepository.findByRecordType(recordType, pageable);
    }

    public Page<ComplianceRecord> getComplianceRecordsByStatus(ComplianceStatus status, Pageable pageable) {
        return complianceRecordRepository.findByStatus(status, pageable);
    }

    public ComplianceRecord getComplianceRecord(UUID id) {
        return complianceRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Compliance record not found: " + id));
    }

    @Transactional
    public ComplianceRecord updateComplianceRecordStatus(UUID id, ComplianceStatus status) {
        ComplianceRecord record = getComplianceRecord(id);
        record.setStatus(status);
        return complianceRecordRepository.save(record);
    }

    public Page<SuspiciousActivity> getSuspiciousActivities(Pageable pageable) {
        return suspiciousActivityRepository.findAll(pageable);
    }

    public Page<SuspiciousActivity> getSuspiciousActivitiesByCustomer(UUID customerId, Pageable pageable) {
        return suspiciousActivityRepository.findByCustomerId(customerId, pageable);
    }

    public Page<SuspiciousActivity> getSuspiciousActivitiesByStatus(
            SuspiciousActivityStatus status,
            Pageable pageable
    ) {
        return suspiciousActivityRepository.findByStatus(status, pageable);
    }

    public SuspiciousActivity getSuspiciousActivity(UUID id) {
        return suspiciousActivityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Suspicious activity not found: " + id));
    }

    @Transactional
    public SuspiciousActivity updateSuspiciousActivityStatus(
            UUID id,
            SuspiciousActivityStatus status,
            UUID investigatorId,
            String notes
    ) {
        SuspiciousActivity activity = getSuspiciousActivity(id);
        activity.setStatus(status);
        if (investigatorId != null) {
            activity.setInvestigatorId(investigatorId);
        }
        if (notes != null && !notes.isBlank()) {
            activity.setInvestigationNotes(notes);
        }
        return suspiciousActivityRepository.save(activity);
    }
}

