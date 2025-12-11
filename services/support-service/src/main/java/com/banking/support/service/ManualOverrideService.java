package com.banking.support.service;

import com.banking.support.domain.ManualOverride;
import com.banking.support.domain.OverrideStatus;
import com.banking.support.domain.OverrideType;
import com.banking.support.messaging.SupportEventPublisher;
import com.banking.support.repository.ManualOverrideRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ManualOverrideService {

    private final ManualOverrideRepository overrideRepository;
    private final SupportEventPublisher eventPublisher;
    private final Clock clock;

    public ManualOverrideService(
            ManualOverrideRepository overrideRepository,
            SupportEventPublisher eventPublisher,
            Clock clock
    ) {
        this.overrideRepository = overrideRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public ManualOverride createOverride(
            OverrideType overrideType,
            UUID customerId,
            UUID accountId,
            UUID entityId,
            String reason,
            String overrideValue,
            BigDecimal amount,
            String currency,
            Instant expiresAt,
            UUID requestedBy
    ) {
        ManualOverride override = new ManualOverride();
        override.setOverrideType(overrideType);
        override.setStatus(OverrideStatus.PENDING);
        override.setCustomerId(customerId);
        override.setAccountId(accountId);
        override.setEntityId(entityId);
        override.setReason(reason);
        override.setOverrideValue(overrideValue);
        override.setAmount(amount);
        override.setCurrency(currency);
        override.setExpiresAt(expiresAt);
        override.setRequestedBy(requestedBy);

        ManualOverride saved = overrideRepository.save(override);
        eventPublisher.publishOverrideCreated(saved);

        return saved;
    }

    public ManualOverride getOverride(UUID overrideId) {
        return overrideRepository.findById(overrideId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Manual override not found: " + overrideId
                ));
    }

    @Transactional
    public ManualOverride approveOverride(UUID overrideId, UUID approvedBy) {
        ManualOverride override = getOverride(overrideId);

        if (override.getStatus() != OverrideStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Override is not in PENDING status"
            );
        }

        override.setStatus(OverrideStatus.APPROVED);
        override.setApprovedBy(approvedBy);
        override.setApprovedAt(Instant.now(clock));

        ManualOverride updated = overrideRepository.save(override);
        eventPublisher.publishOverrideApproved(updated);

        return updated;
    }

    @Transactional
    public ManualOverride rejectOverride(UUID overrideId, String rejectionReason, UUID rejectedBy) {
        ManualOverride override = getOverride(overrideId);

        if (override.getStatus() != OverrideStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Override is not in PENDING status"
            );
        }

        override.setStatus(OverrideStatus.REJECTED);
        override.setRejectedBy(rejectedBy);
        override.setRejectionReason(rejectionReason);
        override.setRejectedAt(Instant.now(clock));

        ManualOverride updated = overrideRepository.save(override);
        eventPublisher.publishOverrideRejected(updated);

        return updated;
    }

    @Transactional
    public ManualOverride revokeOverride(UUID overrideId, UUID revokedBy) {
        ManualOverride override = getOverride(overrideId);

        if (override.getStatus() != OverrideStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only APPROVED overrides can be revoked"
            );
        }

        override.setStatus(OverrideStatus.REVOKED);

        ManualOverride updated = overrideRepository.save(override);
        eventPublisher.publishOverrideRevoked(updated);

        return updated;
    }

    public Page<ManualOverride> getOverridesByStatus(OverrideStatus status, Pageable pageable) {
        return overrideRepository.findByStatus(status, pageable);
    }

    public Page<ManualOverride> getOverridesByType(OverrideType overrideType, Pageable pageable) {
        return overrideRepository.findByOverrideType(overrideType, pageable);
    }

    public Page<ManualOverride> getOverridesByCustomer(UUID customerId, Pageable pageable) {
        return overrideRepository.findByCustomerId(customerId, pageable);
    }

    public Page<ManualOverride> getOverridesByAccount(UUID accountId, Pageable pageable) {
        return overrideRepository.findByAccountId(accountId, pageable);
    }

    public List<ManualOverride> getActiveOverrides(UUID customerId, OverrideType overrideType) {
        return overrideRepository.findByCustomerAndTypeAndStatus(
                customerId,
                overrideType,
                OverrideStatus.APPROVED
        );
    }
}

