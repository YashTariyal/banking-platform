package com.banking.compliance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.compliance.domain.ComplianceRecord;
import com.banking.compliance.domain.ComplianceRecordType;
import com.banking.compliance.domain.ComplianceStatus;
import com.banking.compliance.domain.Severity;
import com.banking.compliance.domain.SuspiciousActivity;
import com.banking.compliance.domain.SuspiciousActivityType;
import com.banking.compliance.repository.ComplianceRecordRepository;
import com.banking.compliance.repository.SuspiciousActivityRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AMLServiceTest {

    @Mock
    private ComplianceRecordRepository complianceRecordRepository;

    @Mock
    private SuspiciousActivityRepository suspiciousActivityRepository;

    private AMLService amlService;

    @BeforeEach
    void setUp() {
        amlService = new AMLService(complianceRecordRepository, suspiciousActivityRepository);
    }

    @Test
    void analyzeTransaction_WithLargeCashAmount_FlagsAsLargeCashTransaction() {
        // Given
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("15000.00");
        String currency = "USD";
        String eventType = "TRANSACTION_COMPLETED";
        String sourceTopic = "transaction-events";

        when(complianceRecordRepository.countByCustomerIdAndStatus(customerId, ComplianceStatus.PENDING))
                .thenReturn(0L);
        when(complianceRecordRepository.save(any(ComplianceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ComplianceRecord result = amlService.analyzeTransaction(
                customerId, accountId, transactionId, amount, currency, eventType, sourceTopic
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecordType()).isEqualTo(ComplianceRecordType.LARGE_CASH_TRANSACTION);
        assertThat(result.getAmount()).isEqualByComparingTo(amount);
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getFlags()).contains("LARGE_CASH_TRANSACTION");
        assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(30);
        verify(complianceRecordRepository).save(any(ComplianceRecord.class));
    }

    @Test
    void analyzeTransaction_WithStructuringAmount_FlagsAsStructuring() {
        // Given
        UUID customerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("9500.00"); // Just below $10k threshold

        when(complianceRecordRepository.countByCustomerIdAndStatus(customerId, ComplianceStatus.PENDING))
                .thenReturn(0L);
        when(complianceRecordRepository.save(any(ComplianceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ComplianceRecord result = amlService.analyzeTransaction(
                customerId, null, null, amount, "USD", "TRANSACTION", "topic"
        );

        // Then
        assertThat(result.getFlags()).contains("STRUCTURING");
        assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(40);
        assertThat(result.getStatus()).isIn(ComplianceStatus.FLAGGED, ComplianceStatus.UNDER_REVIEW);
    }

    @Test
    void analyzeTransaction_WithRoundNumber_FlagsAsRoundNumberPattern() {
        // Given
        UUID customerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("5000.00"); // Round number

        when(complianceRecordRepository.countByCustomerIdAndStatus(customerId, ComplianceStatus.PENDING))
                .thenReturn(0L);
        when(complianceRecordRepository.save(any(ComplianceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ComplianceRecord result = amlService.analyzeTransaction(
                customerId, null, null, amount, "USD", "TRANSACTION", "topic"
        );

        // Then
        assertThat(result.getFlags()).contains("ROUND_NUMBER_PATTERN");
        assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void analyzeTransaction_WithRapidMovement_FlagsAsRapidMovement() {
        // Given
        UUID customerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("1000.00");

        when(complianceRecordRepository.countByCustomerIdAndStatus(customerId, ComplianceStatus.PENDING))
                .thenReturn(10L); // Many recent transactions
        when(complianceRecordRepository.save(any(ComplianceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ComplianceRecord result = amlService.analyzeTransaction(
                customerId, null, null, amount, "USD", "TRANSACTION", "topic"
        );

        // Then
        assertThat(result.getFlags()).contains("RAPID_MOVEMENT");
        assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(20);
    }

    @Test
    void analyzeTransaction_WithHighRiskScore_CreatesSuspiciousActivity() {
        // Given
        UUID customerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("15000.00"); // Large cash + round number = 40 points
        // Add rapid movement to push it over 50 (medium risk threshold)
        when(complianceRecordRepository.countByCustomerIdAndStatus(customerId, ComplianceStatus.PENDING))
                .thenReturn(10L); // Many recent transactions = +20 points, total = 60 (medium risk)
        when(complianceRecordRepository.save(any(ComplianceRecord.class)))
                .thenAnswer(invocation -> {
                    ComplianceRecord record = invocation.getArgument(0);
                    record.setId(UUID.randomUUID());
                    return record;
                });
        when(suspiciousActivityRepository.save(any(SuspiciousActivity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ComplianceRecord record = amlService.analyzeTransaction(
                customerId, null, null, amount, "USD", "TRANSACTION", "topic"
        );

        // Then
        ArgumentCaptor<SuspiciousActivity> activityCaptor = ArgumentCaptor.forClass(SuspiciousActivity.class);
        verify(suspiciousActivityRepository).save(activityCaptor.capture());

        SuspiciousActivity activity = activityCaptor.getValue();
        assertThat(activity).isNotNull();
        assertThat(activity.getCustomerId()).isEqualTo(customerId);
        assertThat(activity.getSeverity()).isIn(Severity.HIGH, Severity.MEDIUM);
        assertThat(activity.getActivityType()).isEqualTo(SuspiciousActivityType.LARGE_CASH_TRANSACTION);
        assertThat(activity.getStatus()).isEqualTo(com.banking.compliance.domain.SuspiciousActivityStatus.OPEN);
    }

    @Test
    void analyzeTransaction_WithLowRiskScore_ClearsRecord() {
        // Given
        UUID customerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00"); // Small amount

        when(complianceRecordRepository.countByCustomerIdAndStatus(customerId, ComplianceStatus.PENDING))
                .thenReturn(0L);
        when(complianceRecordRepository.save(any(ComplianceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ComplianceRecord result = amlService.analyzeTransaction(
                customerId, null, null, amount, "USD", "TRANSACTION", "topic"
        );

        // Then
        assertThat(result.getStatus()).isEqualTo(ComplianceStatus.CLEARED);
        assertThat(result.getRiskScore()).isLessThan(20);
        verify(suspiciousActivityRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void analyzeTransaction_WithMediumRiskScore_SetsUnderReview() {
        // Given
        UUID customerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("9500.00"); // Structuring amount - should trigger medium risk

        when(complianceRecordRepository.countByCustomerIdAndStatus(customerId, ComplianceStatus.PENDING))
                .thenReturn(5L);
        when(complianceRecordRepository.save(any(ComplianceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(suspiciousActivityRepository.save(any(SuspiciousActivity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ComplianceRecord result = amlService.analyzeTransaction(
                customerId, null, null, amount, "USD", "TRANSACTION", "topic"
        );

        // Then
        assertThat(result.getStatus()).isIn(ComplianceStatus.UNDER_REVIEW, ComplianceStatus.FLAGGED);
        assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(50);
    }

    @Test
    void createSuspiciousActivity_SetsCorrectActivityType() {
        // Given
        ComplianceRecord record = new ComplianceRecord();
        record.setId(UUID.randomUUID());
        record.setCustomerId(UUID.randomUUID());
        record.setAmount(new BigDecimal("15000.00"));
        record.setCurrency("USD");
        record.setRiskScore(85);
        record.setFlags("LARGE_CASH_TRANSACTION,ROUND_NUMBER_PATTERN");

        when(suspiciousActivityRepository.save(any(SuspiciousActivity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SuspiciousActivity activity = amlService.createSuspiciousActivity(record, Severity.HIGH);

        // Then
        assertThat(activity).isNotNull();
        assertThat(activity.getActivityType()).isEqualTo(SuspiciousActivityType.LARGE_CASH_TRANSACTION);
        assertThat(activity.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(activity.getRiskScore()).isEqualTo(85);
        assertThat(activity.getComplianceRecordId()).isEqualTo(record.getId());
    }

    @Test
    void createSuspiciousActivity_WithStructuringFlag_SetsStructuringType() {
        // Given
        ComplianceRecord record = new ComplianceRecord();
        record.setId(UUID.randomUUID());
        record.setCustomerId(UUID.randomUUID());
        record.setFlags("STRUCTURING");

        when(suspiciousActivityRepository.save(any(SuspiciousActivity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SuspiciousActivity activity = amlService.createSuspiciousActivity(record, Severity.MEDIUM);

        // Then
        assertThat(activity.getActivityType()).isEqualTo(SuspiciousActivityType.STRUCTURING);
    }
}

