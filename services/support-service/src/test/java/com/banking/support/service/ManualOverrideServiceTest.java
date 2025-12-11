package com.banking.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.support.domain.ManualOverride;
import com.banking.support.domain.OverrideStatus;
import com.banking.support.domain.OverrideType;
import com.banking.support.messaging.SupportEventPublisher;
import com.banking.support.repository.ManualOverrideRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ManualOverrideServiceTest {

    @Mock
    private ManualOverrideRepository overrideRepository;

    @Mock
    private SupportEventPublisher eventPublisher;

    private Clock fixedClock;
    private ManualOverrideService overrideService;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));
        overrideService = new ManualOverrideService(overrideRepository, eventPublisher, fixedClock);
    }

    @Test
    void createOverride_savesWithPendingStatus() {
        UUID customerId = UUID.randomUUID();
        UUID requestedBy = UUID.randomUUID();

        when(overrideRepository.save(any(ManualOverride.class)))
                .thenAnswer(invocation -> {
                    ManualOverride override = invocation.getArgument(0);
                    override.setId(UUID.randomUUID());
                    return override;
                });

        ManualOverride result = overrideService.createOverride(
                OverrideType.ACCOUNT_LIMIT,
                customerId,
                null,
                null,
                "Test reason",
                "10000",
                new BigDecimal("10000"),
                "USD",
                null,
                requestedBy
        );

        assertThat(result).isNotNull();
        assertThat(result.getOverrideType()).isEqualTo(OverrideType.ACCOUNT_LIMIT);
        assertThat(result.getStatus()).isEqualTo(OverrideStatus.PENDING);
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getRequestedBy()).isEqualTo(requestedBy);

        verify(overrideRepository).save(any(ManualOverride.class));
        verify(eventPublisher).publishOverrideCreated(any(ManualOverride.class));
    }

    @Test
    void approveOverride_updatesStatus() {
        UUID overrideId = UUID.randomUUID();
        UUID approvedBy = UUID.randomUUID();
        ManualOverride override = new ManualOverride();
        override.setId(overrideId);
        override.setStatus(OverrideStatus.PENDING);

        when(overrideRepository.findById(overrideId)).thenReturn(Optional.of(override));
        when(overrideRepository.save(any(ManualOverride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ManualOverride result = overrideService.approveOverride(overrideId, approvedBy);

        assertThat(result.getStatus()).isEqualTo(OverrideStatus.APPROVED);
        assertThat(result.getApprovedBy()).isEqualTo(approvedBy);
        assertThat(result.getApprovedAt()).isNotNull();
        verify(eventPublisher).publishOverrideApproved(any(ManualOverride.class));
    }

    @Test
    void approveOverride_notPending_throwsException() {
        UUID overrideId = UUID.randomUUID();
        UUID approvedBy = UUID.randomUUID();
        ManualOverride override = new ManualOverride();
        override.setId(overrideId);
        override.setStatus(OverrideStatus.APPROVED);

        when(overrideRepository.findById(overrideId)).thenReturn(Optional.of(override));

        assertThatThrownBy(() -> overrideService.approveOverride(overrideId, approvedBy))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void rejectOverride_updatesStatus() {
        UUID overrideId = UUID.randomUUID();
        UUID rejectedBy = UUID.randomUUID();
        ManualOverride override = new ManualOverride();
        override.setId(overrideId);
        override.setStatus(OverrideStatus.PENDING);

        when(overrideRepository.findById(overrideId)).thenReturn(Optional.of(override));
        when(overrideRepository.save(any(ManualOverride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ManualOverride result = overrideService.rejectOverride(overrideId, "Rejected", rejectedBy);

        assertThat(result.getStatus()).isEqualTo(OverrideStatus.REJECTED);
        assertThat(result.getRejectedBy()).isEqualTo(rejectedBy);
        assertThat(result.getRejectionReason()).isEqualTo("Rejected");
        assertThat(result.getRejectedAt()).isNotNull();
        verify(eventPublisher).publishOverrideRejected(any(ManualOverride.class));
    }

    @Test
    void revokeOverride_updatesStatus() {
        UUID overrideId = UUID.randomUUID();
        UUID revokedBy = UUID.randomUUID();
        ManualOverride override = new ManualOverride();
        override.setId(overrideId);
        override.setStatus(OverrideStatus.APPROVED);

        when(overrideRepository.findById(overrideId)).thenReturn(Optional.of(override));
        when(overrideRepository.save(any(ManualOverride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ManualOverride result = overrideService.revokeOverride(overrideId, revokedBy);

        assertThat(result.getStatus()).isEqualTo(OverrideStatus.REVOKED);
        verify(eventPublisher).publishOverrideRevoked(any(ManualOverride.class));
    }

    @Test
    void revokeOverride_notApproved_throwsException() {
        UUID overrideId = UUID.randomUUID();
        UUID revokedBy = UUID.randomUUID();
        ManualOverride override = new ManualOverride();
        override.setId(overrideId);
        override.setStatus(OverrideStatus.PENDING);

        when(overrideRepository.findById(overrideId)).thenReturn(Optional.of(override));

        assertThatThrownBy(() -> overrideService.revokeOverride(overrideId, revokedBy))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void getActiveOverrides_returnsApprovedOverrides() {
        UUID customerId = UUID.randomUUID();
        ManualOverride override = new ManualOverride();
        override.setId(UUID.randomUUID());
        override.setStatus(OverrideStatus.APPROVED);

        when(overrideRepository.findByCustomerAndTypeAndStatus(
                customerId, OverrideType.ACCOUNT_LIMIT, OverrideStatus.APPROVED))
                .thenReturn(List.of(override));

        List<ManualOverride> result = overrideService.getActiveOverrides(
                customerId, OverrideType.ACCOUNT_LIMIT);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(OverrideStatus.APPROVED);
    }
}

