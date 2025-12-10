package com.banking.compliance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.compliance.domain.ComplianceRecord;
import com.banking.compliance.domain.ComplianceRecordType;
import com.banking.compliance.domain.ComplianceStatus;
import com.banking.compliance.domain.SuspiciousActivity;
import com.banking.compliance.domain.SuspiciousActivityStatus;
import com.banking.compliance.repository.ComplianceRecordRepository;
import com.banking.compliance.repository.SuspiciousActivityRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
class ComplianceServiceTest {

    @Mock
    private ComplianceRecordRepository complianceRecordRepository;

    @Mock
    private SuspiciousActivityRepository suspiciousActivityRepository;

    private ComplianceService complianceService;

    @BeforeEach
    void setUp() {
        complianceService = new ComplianceService(complianceRecordRepository, suspiciousActivityRepository);
    }

    @Test
    void getComplianceRecordsByCustomer_ReturnsPaginatedResults() {
        // Given
        UUID customerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        ComplianceRecord record = createComplianceRecord();
        record.setCustomerId(customerId); // Ensure customerId matches
        Page<ComplianceRecord> page = new PageImpl<>(java.util.List.of(record));

        when(complianceRecordRepository.findByCustomerId(customerId, pageable)).thenReturn(page);

        // When
        Page<ComplianceRecord> result = complianceService.getComplianceRecordsByCustomer(customerId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCustomerId()).isEqualTo(customerId);
        verify(complianceRecordRepository).findByCustomerId(customerId, pageable);
    }

    @Test
    void getComplianceRecordsByAccount_ReturnsPaginatedResults() {
        // Given
        UUID accountId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        ComplianceRecord record = createComplianceRecord();
        record.setAccountId(accountId);
        Page<ComplianceRecord> page = new PageImpl<>(java.util.List.of(record));

        when(complianceRecordRepository.findByAccountId(accountId, pageable)).thenReturn(page);

        // When
        Page<ComplianceRecord> result = complianceService.getComplianceRecordsByAccount(accountId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAccountId()).isEqualTo(accountId);
    }

    @Test
    void getComplianceRecord_WhenExists_ReturnsRecord() {
        // Given
        UUID id = UUID.randomUUID();
        ComplianceRecord record = createComplianceRecord();
        record.setId(id);

        when(complianceRecordRepository.findById(id)).thenReturn(Optional.of(record));

        // When
        ComplianceRecord result = complianceService.getComplianceRecord(id);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void getComplianceRecord_WhenNotExists_ThrowsException() {
        // Given
        UUID id = UUID.randomUUID();
        when(complianceRecordRepository.findById(id)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> complianceService.getComplianceRecord(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void updateComplianceRecordStatus_UpdatesStatus() {
        // Given
        UUID id = UUID.randomUUID();
        ComplianceRecord record = createComplianceRecord();
        record.setId(id);
        record.setStatus(ComplianceStatus.PENDING);

        when(complianceRecordRepository.findById(id)).thenReturn(Optional.of(record));
        when(complianceRecordRepository.save(any(ComplianceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ComplianceRecord result = complianceService.updateComplianceRecordStatus(id, ComplianceStatus.FLAGGED);

        // Then
        assertThat(result.getStatus()).isEqualTo(ComplianceStatus.FLAGGED);
        verify(complianceRecordRepository).save(record);
    }

    @Test
    void getSuspiciousActivities_ReturnsPaginatedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        SuspiciousActivity activity = createSuspiciousActivity();
        Page<SuspiciousActivity> page = new PageImpl<>(java.util.List.of(activity));

        when(suspiciousActivityRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<SuspiciousActivity> result = complianceService.getSuspiciousActivities(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getSuspiciousActivitiesByCustomer_ReturnsFilteredResults() {
        // Given
        UUID customerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        SuspiciousActivity activity = createSuspiciousActivity();
        activity.setCustomerId(customerId);
        Page<SuspiciousActivity> page = new PageImpl<>(java.util.List.of(activity));

        when(suspiciousActivityRepository.findByCustomerId(customerId, pageable)).thenReturn(page);

        // When
        Page<SuspiciousActivity> result = complianceService.getSuspiciousActivitiesByCustomer(customerId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCustomerId()).isEqualTo(customerId);
    }

    @Test
    void getSuspiciousActivity_WhenExists_ReturnsActivity() {
        // Given
        UUID id = UUID.randomUUID();
        SuspiciousActivity activity = createSuspiciousActivity();
        activity.setId(id);

        when(suspiciousActivityRepository.findById(id)).thenReturn(Optional.of(activity));

        // When
        SuspiciousActivity result = complianceService.getSuspiciousActivity(id);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void getSuspiciousActivity_WhenNotExists_ThrowsException() {
        // Given
        UUID id = UUID.randomUUID();
        when(suspiciousActivityRepository.findById(id)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> complianceService.getSuspiciousActivity(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void updateSuspiciousActivityStatus_UpdatesStatusAndInvestigator() {
        // Given
        UUID id = UUID.randomUUID();
        UUID investigatorId = UUID.randomUUID();
        SuspiciousActivity activity = createSuspiciousActivity();
        activity.setId(id);
        activity.setStatus(SuspiciousActivityStatus.OPEN);

        when(suspiciousActivityRepository.findById(id)).thenReturn(Optional.of(activity));
        when(suspiciousActivityRepository.save(any(SuspiciousActivity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SuspiciousActivity result = complianceService.updateSuspiciousActivityStatus(
                id,
                SuspiciousActivityStatus.UNDER_INVESTIGATION,
                investigatorId,
                "Investigation notes"
        );

        // Then
        assertThat(result.getStatus()).isEqualTo(SuspiciousActivityStatus.UNDER_INVESTIGATION);
        assertThat(result.getInvestigatorId()).isEqualTo(investigatorId);
        assertThat(result.getInvestigationNotes()).isEqualTo("Investigation notes");
        verify(suspiciousActivityRepository).save(activity);
    }

    private ComplianceRecord createComplianceRecord() {
        ComplianceRecord record = new ComplianceRecord();
        record.setId(UUID.randomUUID());
        record.setCustomerId(UUID.randomUUID());
        record.setRecordType(ComplianceRecordType.TRANSACTION_MONITORING);
        record.setStatus(ComplianceStatus.PENDING);
        record.setAmount(new BigDecimal("1000.00"));
        record.setCurrency("USD");
        record.setRiskScore(50);
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        return record;
    }

    private SuspiciousActivity createSuspiciousActivity() {
        SuspiciousActivity activity = new SuspiciousActivity();
        activity.setId(UUID.randomUUID());
        activity.setCustomerId(UUID.randomUUID());
        activity.setActivityType(com.banking.compliance.domain.SuspiciousActivityType.UNUSUAL_PATTERN);
        activity.setSeverity(com.banking.compliance.domain.Severity.MEDIUM);
        activity.setStatus(SuspiciousActivityStatus.OPEN);
        activity.setRiskScore(60);
        activity.setDescription("Test suspicious activity");
        activity.setCreatedAt(Instant.now());
        activity.setUpdatedAt(Instant.now());
        return activity;
    }
}

