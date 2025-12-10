package com.banking.kyc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.kyc.domain.KYCCase;
import com.banking.kyc.domain.ScreeningResult;
import com.banking.kyc.domain.ScreeningResultStatus;
import com.banking.kyc.domain.ScreeningType;
import com.banking.kyc.repository.KYCCaseRepository;
import com.banking.kyc.repository.ScreeningResultRepository;
import com.banking.kyc.web.dto.CreateScreeningRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScreeningServiceTest {

    @Mock
    private ScreeningResultRepository screeningResultRepository;

    @Mock
    private KYCCaseRepository kycCaseRepository;

    @Mock
    private KYCService kycService;

    private ScreeningService screeningService;

    @BeforeEach
    void setUp() {
        screeningService = new ScreeningService(screeningResultRepository, kycCaseRepository, kycService);
    }

    @Test
    void performScreening_CreatesScreeningResult() {
        // Given
        UUID kycCaseId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CreateScreeningRequest request = new CreateScreeningRequest(
                kycCaseId, customerId, ScreeningType.SANCTIONS, ScreeningResultStatus.CLEAR,
                null, null, null, null, null, null
        );

        KYCCase kycCase = new KYCCase();
        kycCase.setId(kycCaseId);

        when(kycCaseRepository.findById(kycCaseId)).thenReturn(Optional.of(kycCase));
        when(screeningResultRepository.findByKycCaseIdAndScreeningType(any(), any())).thenReturn(List.of());
        when(screeningResultRepository.save(any(ScreeningResult.class))).thenAnswer(invocation -> {
            ScreeningResult result = invocation.getArgument(0);
            result.setId(UUID.randomUUID());
            return result;
        });

        // When
        ScreeningResult result = screeningService.performScreening(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getScreeningType()).isEqualTo(ScreeningType.SANCTIONS);
        assertThat(result.getResult()).isEqualTo(ScreeningResultStatus.CLEAR);
        verify(screeningResultRepository).save(any(ScreeningResult.class));
    }

    @Test
    void performScreening_WithInvalidCase_ThrowsException() {
        // Given
        UUID kycCaseId = UUID.randomUUID();
        CreateScreeningRequest request = new CreateScreeningRequest(
                kycCaseId, UUID.randomUUID(), ScreeningType.SANCTIONS, null,
                null, null, null, null, null, null
        );

        when(kycCaseRepository.findById(kycCaseId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> screeningService.performScreening(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void reviewScreeningResult_UpdatesResult() {
        // Given
        UUID screeningResultId = UUID.randomUUID();
        ScreeningResult result = createScreeningResult();
        result.setId(screeningResultId);

        when(screeningResultRepository.findById(screeningResultId)).thenReturn(Optional.of(result));
        when(screeningResultRepository.save(any(ScreeningResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ScreeningResult reviewed = screeningService.reviewScreeningResult(
                screeningResultId, UUID.randomUUID(), "False positive", ScreeningResultStatus.FALSE_POSITIVE
        );

        // Then
        assertThat(reviewed.getResult()).isEqualTo(ScreeningResultStatus.FALSE_POSITIVE);
        assertThat(reviewed.getReviewedAt()).isNotNull();
        verify(screeningResultRepository).save(any(ScreeningResult.class));
    }

    private ScreeningResult createScreeningResult() {
        ScreeningResult result = new ScreeningResult();
        result.setId(UUID.randomUUID());
        result.setKycCaseId(UUID.randomUUID());
        result.setCustomerId(UUID.randomUUID());
        result.setScreeningType(ScreeningType.SANCTIONS);
        result.setResult(ScreeningResultStatus.MATCH);
        return result;
    }
}

