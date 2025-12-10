package com.banking.compliance.service;

import com.banking.compliance.domain.ComplianceRecord;
import com.banking.compliance.domain.ComplianceRecordType;
import com.banking.compliance.domain.ComplianceStatus;
import com.banking.compliance.domain.Severity;
import com.banking.compliance.domain.SuspiciousActivity;
import com.banking.compliance.domain.SuspiciousActivityStatus;
import com.banking.compliance.domain.SuspiciousActivityType;
import com.banking.compliance.repository.ComplianceRecordRepository;
import com.banking.compliance.repository.SuspiciousActivityRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AMLService {

    private static final Logger log = LoggerFactory.getLogger(AMLService.class);
    private static final BigDecimal LARGE_CASH_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal STRUCTURING_THRESHOLD = new BigDecimal("9000");
    private static final int HIGH_RISK_SCORE = 80;
    private static final int MEDIUM_RISK_SCORE = 50;
    private static final int LOW_RISK_SCORE = 20;

    private final ComplianceRecordRepository complianceRecordRepository;
    private final SuspiciousActivityRepository suspiciousActivityRepository;

    public AMLService(
            ComplianceRecordRepository complianceRecordRepository,
            SuspiciousActivityRepository suspiciousActivityRepository
    ) {
        this.complianceRecordRepository = complianceRecordRepository;
        this.suspiciousActivityRepository = suspiciousActivityRepository;
    }

    @Transactional
    public ComplianceRecord analyzeTransaction(
            UUID customerId,
            UUID accountId,
            UUID transactionId,
            BigDecimal amount,
            String currency,
            String eventType,
            String sourceTopic
    ) {
        log.debug("Analyzing transaction {} for customer {} with amount {}", transactionId, customerId, amount);

        ComplianceRecord record = new ComplianceRecord();
        record.setCustomerId(customerId);
        record.setAccountId(accountId);
        record.setTransactionId(transactionId);
        record.setRecordType(ComplianceRecordType.TRANSACTION_MONITORING);
        record.setAmount(amount);
        record.setCurrency(currency);
        record.setSourceEventType(eventType);
        record.setSourceTopic(sourceTopic);

        List<String> flags = new ArrayList<>();
        int riskScore = 0;

        // Check for large cash transactions
        if (amount.compareTo(LARGE_CASH_THRESHOLD) >= 0) {
            flags.add("LARGE_CASH_TRANSACTION");
            riskScore += 30;
            record.setRecordType(ComplianceRecordType.LARGE_CASH_TRANSACTION);
        }

        // Check for structuring (amounts just below threshold)
        if (amount.compareTo(STRUCTURING_THRESHOLD) >= 0 && amount.compareTo(LARGE_CASH_THRESHOLD) < 0) {
            flags.add("STRUCTURING");
            riskScore += 40;
        }

        // Check for round numbers (potential structuring)
        if (isRoundNumber(amount)) {
            flags.add("ROUND_NUMBER_PATTERN");
            riskScore += 10;
        }

        // Check transaction frequency (simplified - would need historical data)
        long recentTransactions = complianceRecordRepository
                .countByCustomerIdAndStatus(customerId, ComplianceStatus.PENDING);
        if (recentTransactions > 5) {
            flags.add("RAPID_MOVEMENT");
            riskScore += 20;
        }

        record.setFlags(String.join(",", flags));
        record.setRiskScore(riskScore);

        if (riskScore >= HIGH_RISK_SCORE) {
            record.setStatus(ComplianceStatus.FLAGGED);
        } else if (riskScore >= MEDIUM_RISK_SCORE) {
            record.setStatus(ComplianceStatus.UNDER_REVIEW);
        } else if (riskScore >= LOW_RISK_SCORE) {
            record.setStatus(ComplianceStatus.PENDING);
        } else {
            record.setStatus(ComplianceStatus.CLEARED);
        }

        record.setDescription(buildDescription(flags, riskScore));
        ComplianceRecord savedRecord = complianceRecordRepository.save(record);

        if (riskScore >= HIGH_RISK_SCORE) {
            createSuspiciousActivity(savedRecord, Severity.HIGH);
        } else if (riskScore >= MEDIUM_RISK_SCORE) {
            createSuspiciousActivity(savedRecord, Severity.MEDIUM);
        }

        return savedRecord;
    }

    @Transactional
    public SuspiciousActivity createSuspiciousActivity(ComplianceRecord record, Severity severity) {
        SuspiciousActivity activity = new SuspiciousActivity();
        activity.setCustomerId(record.getCustomerId());
        activity.setAccountId(record.getAccountId());
        activity.setTransactionId(record.getTransactionId());
        activity.setComplianceRecordId(record.getId());
        activity.setAmount(record.getAmount());
        activity.setCurrency(record.getCurrency());
        activity.setRiskScore(record.getRiskScore());
        activity.setSeverity(severity);
        activity.setStatus(SuspiciousActivityStatus.OPEN);

        // Determine activity type based on flags
        String flags = record.getFlags();
        if (flags != null) {
            if (flags.contains("LARGE_CASH_TRANSACTION")) {
                activity.setActivityType(SuspiciousActivityType.LARGE_CASH_TRANSACTION);
            } else if (flags.contains("STRUCTURING")) {
                activity.setActivityType(SuspiciousActivityType.STRUCTURING);
            } else if (flags.contains("RAPID_MOVEMENT")) {
                activity.setActivityType(SuspiciousActivityType.RAPID_MOVEMENT);
            } else if (flags.contains("ROUND_NUMBER_PATTERN")) {
                activity.setActivityType(SuspiciousActivityType.ROUND_NUMBER_PATTERN);
            } else {
                activity.setActivityType(SuspiciousActivityType.UNUSUAL_PATTERN);
            }
        } else {
            activity.setActivityType(SuspiciousActivityType.UNUSUAL_PATTERN);
        }

        activity.setDescription(record.getDescription());
        return suspiciousActivityRepository.save(activity);
    }

    private boolean isRoundNumber(BigDecimal amount) {
        // Check if amount is a round number (ends in .00 or .0000)
        BigDecimal remainder = amount.remainder(BigDecimal.ONE);
        return remainder.compareTo(BigDecimal.ZERO) == 0 ||
               remainder.compareTo(new BigDecimal("0.00")) == 0 ||
               remainder.compareTo(new BigDecimal("0.0000")) == 0;
    }

    private String buildDescription(List<String> flags, int riskScore) {
        StringBuilder desc = new StringBuilder("AML Analysis - Risk Score: ").append(riskScore);
        if (!flags.isEmpty()) {
            desc.append(", Flags: ").append(String.join(", ", flags));
        }
        return desc.toString();
    }
}

