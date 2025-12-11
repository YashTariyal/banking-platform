package com.banking.risk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.risk.domain.AlertStatus;
import com.banking.risk.domain.RiskAlert;
import com.banking.risk.domain.RiskLevel;
import com.banking.risk.messaging.RiskEventPublisher;
import com.banking.risk.repository.RiskAlertRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class RiskAlertServiceTest {

    @Mock
    private RiskAlertRepository alertRepository;

    @Mock
    private RiskEventPublisher eventPublisher;

    private Clock fixedClock;
    private RiskAlertService alertService;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));
        alertService = new RiskAlertService(alertRepository, eventPublisher, fixedClock);
    }

    @Test
    void getAlert_exists_returnsAlert() {
        UUID alertId = UUID.randomUUID();
        RiskAlert alert = new RiskAlert();
        alert.setId(alertId);
        alert.setStatus(AlertStatus.OPEN);

        when(alertRepository.findById(alertId))
                .thenReturn(Optional.of(alert));

        RiskAlert result = alertService.getAlert(alertId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(alertId);
    }

    @Test
    void getAlert_notExists_throwsException() {
        UUID alertId = UUID.randomUUID();

        when(alertRepository.findById(alertId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.getAlert(alertId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void updateAlertStatus_resolved_setsReviewedFields() {
        UUID alertId = UUID.randomUUID();
        UUID reviewedBy = UUID.randomUUID();
        RiskAlert alert = new RiskAlert();
        alert.setId(alertId);
        alert.setStatus(AlertStatus.OPEN);

        when(alertRepository.findById(alertId))
                .thenReturn(Optional.of(alert));
        when(alertRepository.save(any(RiskAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RiskAlert result = alertService.updateAlertStatus(
                alertId,
                AlertStatus.RESOLVED,
                reviewedBy,
                "Resolved as false positive"
        );

        assertThat(result.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(result.getReviewedBy()).isEqualTo(reviewedBy);
        assertThat(result.getReviewedAt()).isNotNull();
        assertThat(result.getResolutionNotes()).isEqualTo("Resolved as false positive");

        verify(eventPublisher).publishRiskAlertUpdated(any(RiskAlert.class));
    }

    @Test
    void updateAlertStatus_underReview_doesNotSetReviewedFields() {
        UUID alertId = UUID.randomUUID();
        RiskAlert alert = new RiskAlert();
        alert.setId(alertId);
        alert.setStatus(AlertStatus.OPEN);

        when(alertRepository.findById(alertId))
                .thenReturn(Optional.of(alert));
        when(alertRepository.save(any(RiskAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RiskAlert result = alertService.updateAlertStatus(
                alertId,
                AlertStatus.UNDER_REVIEW,
                null,
                null
        );

        assertThat(result.getStatus()).isEqualTo(AlertStatus.UNDER_REVIEW);
        assertThat(result.getReviewedBy()).isNull();
        assertThat(result.getReviewedAt()).isNull();
    }

    @Test
    void getAlertsByStatus_returnsPage() {
        PageRequest pageable = PageRequest.of(0, 20);
        RiskAlert alert = new RiskAlert();
        alert.setId(UUID.randomUUID());
        alert.setStatus(AlertStatus.OPEN);

        when(alertRepository.findByStatus(AlertStatus.OPEN, pageable))
                .thenReturn(new PageImpl<>(List.of(alert), pageable, 1));

        Page<RiskAlert> result = alertService.getAlertsByStatus(AlertStatus.OPEN, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getOpenAlerts_returnsPage() {
        PageRequest pageable = PageRequest.of(0, 20);
        RiskAlert alert = new RiskAlert();
        alert.setId(UUID.randomUUID());
        alert.setStatus(AlertStatus.OPEN);

        when(alertRepository.findByStatus(AlertStatus.OPEN, pageable))
                .thenReturn(new PageImpl<>(List.of(alert), pageable, 1));

        Page<RiskAlert> result = alertService.getOpenAlerts(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }
}

