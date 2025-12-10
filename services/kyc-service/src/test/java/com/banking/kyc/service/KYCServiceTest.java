package com.banking.kyc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.kyc.domain.KYCStatus;
import com.banking.kyc.domain.KYCCase;
import com.banking.kyc.domain.RiskLevel;
import com.banking.kyc.messaging.KYCEventPublisher;
import com.banking.kyc.repository.KYCCaseRepository;
import com.banking.kyc.web.dto.CreateKYCCaseRequest;
import com.banking.kyc.web.dto.UpdateKYCCaseRequest;
import java.time.Instant;
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
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class KYCServiceTest {

    @Mock
    private KYCCaseRepository kycCaseRepository;

    @Mock
    private KYCEventPublisher eventPublisher;

    private KYCService kycService;

    @BeforeEach
    void setUp() {
        kycService = new KYCService(kycCaseRepository, eventPublisher);
    }

    @Test
    void createKYCCase_CreatesNewCase() {
        // Given
        UUID customerId = UUID.randomUUID();
        CreateKYCCaseRequest request = new CreateKYCCaseRequest(
                customerId, "ONBOARDING", RiskLevel.LOW, null, null
        );

        when(kycCaseRepository.findByCustomerIdAndStatusNotIn(any(), any())).thenReturn(Optional.empty());
        when(kycCaseRepository.save(any(KYCCase.class))).thenAnswer(invocation -> {
            KYCCase kycCase = invocation.getArgument(0);
            kycCase.setId(UUID.randomUUID());
            return kycCase;
        });

        // When
        KYCCase result = kycService.createKYCCase(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getStatus()).isEqualTo(KYCStatus.PENDING);
        assertThat(result.getCaseType()).isEqualTo("ONBOARDING");
        verify(kycCaseRepository).save(any(KYCCase.class));
        verify(eventPublisher).publishKYCCaseCreated(any(KYCCase.class));
    }

    @Test
    void createKYCCase_WithActiveCase_ThrowsException() {
        // Given
        UUID customerId = UUID.randomUUID();
        CreateKYCCaseRequest request = new CreateKYCCaseRequest(
                customerId, "ONBOARDING", RiskLevel.LOW, null, null
        );

        KYCCase existing = createKYCCase();
        when(kycCaseRepository.findByCustomerIdAndStatusNotIn(any(), any())).thenReturn(Optional.of(existing));

        // When/Then
        assertThatThrownBy(() -> kycService.createKYCCase(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void approveKYCCase_WithCompletedRequirements_Approves() {
        // Given
        UUID id = UUID.randomUUID();
        KYCCase kycCase = createKYCCase();
        kycCase.setId(id);
        kycCase.setScreeningCompleted(true);
        kycCase.setDocumentVerificationCompleted(true);

        when(kycCaseRepository.findById(id)).thenReturn(Optional.of(kycCase));
        when(kycCaseRepository.save(any(KYCCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        KYCCase approved = kycService.approveKYCCase(id, UUID.randomUUID(), "Approved");

        // Then
        assertThat(approved.getStatus()).isEqualTo(KYCStatus.APPROVED);
        assertThat(approved.getApprovedAt()).isNotNull();
        verify(eventPublisher).publishKYCCaseApproved(any(KYCCase.class));
    }

    @Test
    void approveKYCCase_WithoutCompletedRequirements_ThrowsException() {
        // Given
        UUID id = UUID.randomUUID();
        KYCCase kycCase = createKYCCase();
        kycCase.setId(id);
        kycCase.setScreeningCompleted(false);

        when(kycCaseRepository.findById(id)).thenReturn(Optional.of(kycCase));

        // When/Then
        assertThatThrownBy(() -> kycService.approveKYCCase(id, UUID.randomUUID(), "Notes"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot approve");
    }

    @Test
    void rejectKYCCase_RejectsCase() {
        // Given
        UUID id = UUID.randomUUID();
        KYCCase kycCase = createKYCCase();
        kycCase.setId(id);

        when(kycCaseRepository.findById(id)).thenReturn(Optional.of(kycCase));
        when(kycCaseRepository.save(any(KYCCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        KYCCase rejected = kycService.rejectKYCCase(id, UUID.randomUUID(), "Rejection reason");

        // Then
        assertThat(rejected.getStatus()).isEqualTo(KYCStatus.REJECTED);
        assertThat(rejected.getRejectedAt()).isNotNull();
        assertThat(rejected.getRejectionReason()).isEqualTo("Rejection reason");
        verify(eventPublisher).publishKYCCaseRejected(any(KYCCase.class));
    }

    @Test
    void markScreeningCompleted_UpdatesStatus() {
        // Given
        UUID id = UUID.randomUUID();
        KYCCase kycCase = createKYCCase();
        kycCase.setId(id);
        kycCase.setDocumentVerificationCompleted(true);

        when(kycCaseRepository.findById(id)).thenReturn(Optional.of(kycCase));
        when(kycCaseRepository.save(any(KYCCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        kycService.markScreeningCompleted(id);

        // Then
        verify(kycCaseRepository).save(any(KYCCase.class));
    }

    private KYCCase createKYCCase() {
        KYCCase kycCase = new KYCCase();
        kycCase.setId(UUID.randomUUID());
        kycCase.setCustomerId(UUID.randomUUID());
        kycCase.setStatus(KYCStatus.PENDING);
        kycCase.setCaseType("ONBOARDING");
        kycCase.setRiskLevel(RiskLevel.LOW);
        kycCase.setScreeningCompleted(false);
        kycCase.setDocumentVerificationCompleted(false);
        kycCase.setCreatedAt(Instant.now());
        kycCase.setUpdatedAt(Instant.now());
        return kycCase;
    }
}

