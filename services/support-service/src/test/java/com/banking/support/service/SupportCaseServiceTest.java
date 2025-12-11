package com.banking.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.support.domain.CasePriority;
import com.banking.support.domain.CaseStatus;
import com.banking.support.domain.CaseType;
import com.banking.support.domain.SupportCase;
import com.banking.support.messaging.SupportEventPublisher;
import com.banking.support.repository.SupportCaseRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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
class SupportCaseServiceTest {

    @Mock
    private SupportCaseRepository caseRepository;

    @Mock
    private CaseNumberGenerator caseNumberGenerator;

    @Mock
    private SupportEventPublisher eventPublisher;

    private Clock fixedClock;
    private SupportCaseService caseService;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));
        caseService = new SupportCaseService(
                caseRepository,
                caseNumberGenerator,
                eventPublisher,
                fixedClock
        );
    }

    @Test
    void createCase_generatesCaseNumberAndSaves() {
        UUID customerId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        String caseNumber = "CASE1234567890";

        when(caseNumberGenerator.generateCaseNumber()).thenReturn(caseNumber);
        when(caseRepository.save(any(SupportCase.class)))
                .thenAnswer(invocation -> {
                    SupportCase supportCase = invocation.getArgument(0);
                    supportCase.setId(UUID.randomUUID());
                    return supportCase;
                });

        SupportCase result = caseService.createCase(
                CaseType.ACCOUNT_INQUIRY,
                CasePriority.HIGH,
                customerId,
                null,
                "Test Case",
                "Test description",
                createdBy,
                null
        );

        assertThat(result).isNotNull();
        assertThat(result.getCaseNumber()).isEqualTo(caseNumber);
        assertThat(result.getCaseType()).isEqualTo(CaseType.ACCOUNT_INQUIRY);
        assertThat(result.getPriority()).isEqualTo(CasePriority.HIGH);
        assertThat(result.getStatus()).isEqualTo(CaseStatus.OPEN);
        assertThat(result.getCustomerId()).isEqualTo(customerId);

        verify(caseRepository).save(any(SupportCase.class));
        verify(eventPublisher).publishCaseCreated(any(SupportCase.class));
    }

    @Test
    void assignCase_updatesStatus() {
        UUID caseId = UUID.randomUUID();
        UUID assignedTo = UUID.randomUUID();
        SupportCase supportCase = new SupportCase();
        supportCase.setId(caseId);
        supportCase.setStatus(CaseStatus.OPEN);

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(supportCase));
        when(caseRepository.save(any(SupportCase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SupportCase result = caseService.assignCase(caseId, assignedTo);

        assertThat(result.getAssignedTo()).isEqualTo(assignedTo);
        assertThat(result.getStatus()).isEqualTo(CaseStatus.ASSIGNED);
        verify(eventPublisher).publishCaseUpdated(any(SupportCase.class));
    }

    @Test
    void resolveCase_setsResolvedFields() {
        UUID caseId = UUID.randomUUID();
        UUID resolvedBy = UUID.randomUUID();
        SupportCase supportCase = new SupportCase();
        supportCase.setId(caseId);
        supportCase.setStatus(CaseStatus.IN_PROGRESS);

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(supportCase));
        when(caseRepository.save(any(SupportCase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SupportCase result = caseService.resolveCase(caseId, "Resolved", resolvedBy);

        assertThat(result.getStatus()).isEqualTo(CaseStatus.RESOLVED);
        assertThat(result.getResolutionNotes()).isEqualTo("Resolved");
        assertThat(result.getResolvedBy()).isEqualTo(resolvedBy);
        assertThat(result.getResolvedAt()).isNotNull();
        verify(eventPublisher).publishCaseResolved(any(SupportCase.class));
    }

    @Test
    void getCase_notExists_throwsException() {
        UUID caseId = UUID.randomUUID();

        when(caseRepository.findById(caseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caseService.getCase(caseId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void updateStatus_resolved_setsResolvedFields() {
        UUID caseId = UUID.randomUUID();
        UUID updatedBy = UUID.randomUUID();
        SupportCase supportCase = new SupportCase();
        supportCase.setId(caseId);
        supportCase.setStatus(CaseStatus.IN_PROGRESS);

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(supportCase));
        when(caseRepository.save(any(SupportCase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SupportCase result = caseService.updateStatus(caseId, CaseStatus.RESOLVED, updatedBy);

        assertThat(result.getStatus()).isEqualTo(CaseStatus.RESOLVED);
        assertThat(result.getResolvedBy()).isEqualTo(updatedBy);
        assertThat(result.getResolvedAt()).isNotNull();
        verify(eventPublisher).publishCaseUpdated(any(SupportCase.class));
    }

    @Test
    void updateStatus_nonResolved_doesNotSetResolvedFields() {
        UUID caseId = UUID.randomUUID();
        SupportCase supportCase = new SupportCase();
        supportCase.setId(caseId);
        supportCase.setStatus(CaseStatus.OPEN);

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(supportCase));
        when(caseRepository.save(any(SupportCase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SupportCase result = caseService.updateStatus(caseId, CaseStatus.IN_PROGRESS, null);

        assertThat(result.getStatus()).isEqualTo(CaseStatus.IN_PROGRESS);
        assertThat(result.getResolvedBy()).isNull();
        assertThat(result.getResolvedAt()).isNull();
    }
}

