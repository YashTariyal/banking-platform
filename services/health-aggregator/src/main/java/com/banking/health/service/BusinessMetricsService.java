package com.banking.health.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BusinessMetricsService {

    private static final Logger log = LoggerFactory.getLogger(BusinessMetricsService.class);

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicLong> gaugeValues = new ConcurrentHashMap<>();

    // Transaction metrics
    private final Counter transactionsTotal;
    private final Counter transactionsApproved;
    private final Counter transactionsDeclined;
    private final Timer transactionProcessingTime;

    // Account metrics
    private final Counter accountsCreated;
    private final Counter accountsClosed;

    // Loan metrics
    private final Counter loanApplicationsTotal;
    private final Counter loanApplicationsApproved;
    private final Counter loanApplicationsRejected;

    // Authentication metrics
    private final Counter loginAttempts;
    private final Counter loginSuccesses;
    private final Counter loginFailures;

    public BusinessMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Transaction counters
        this.transactionsTotal = Counter.builder("banking.transactions.total")
                .description("Total number of transactions")
                .register(meterRegistry);

        this.transactionsApproved = Counter.builder("banking.transactions.approved")
                .description("Approved transactions")
                .register(meterRegistry);

        this.transactionsDeclined = Counter.builder("banking.transactions.declined")
                .description("Declined transactions")
                .register(meterRegistry);

        this.transactionProcessingTime = Timer.builder("banking.transactions.processing.time")
                .description("Transaction processing time")
                .register(meterRegistry);

        // Account counters
        this.accountsCreated = Counter.builder("banking.accounts.created")
                .description("Accounts created")
                .register(meterRegistry);

        this.accountsClosed = Counter.builder("banking.accounts.closed")
                .description("Accounts closed")
                .register(meterRegistry);

        // Loan counters
        this.loanApplicationsTotal = Counter.builder("banking.loans.applications.total")
                .description("Total loan applications")
                .register(meterRegistry);

        this.loanApplicationsApproved = Counter.builder("banking.loans.applications.approved")
                .description("Approved loan applications")
                .register(meterRegistry);

        this.loanApplicationsRejected = Counter.builder("banking.loans.applications.rejected")
                .description("Rejected loan applications")
                .register(meterRegistry);

        // Auth counters
        this.loginAttempts = Counter.builder("banking.auth.login.attempts")
                .description("Login attempts")
                .register(meterRegistry);

        this.loginSuccesses = Counter.builder("banking.auth.login.successes")
                .description("Successful logins")
                .register(meterRegistry);

        this.loginFailures = Counter.builder("banking.auth.login.failures")
                .description("Failed logins")
                .register(meterRegistry);

        // Register dynamic gauges
        registerGauge("active.users", "Number of currently active users");
        registerGauge("pending.transactions", "Number of pending transactions");
        registerGauge("active.sessions", "Number of active sessions");
    }

    private void registerGauge(String name, String description) {
        AtomicLong value = new AtomicLong(0);
        gaugeValues.put(name, value);
        Gauge.builder("banking." + name, value::get)
                .description(description)
                .register(meterRegistry);
    }

    // Transaction metrics
    public void recordTransaction(boolean approved, long processingTimeMs) {
        transactionsTotal.increment();
        if (approved) {
            transactionsApproved.increment();
        } else {
            transactionsDeclined.increment();
        }
        transactionProcessingTime.record(Duration.ofMillis(processingTimeMs));
    }

    // Account metrics
    public void recordAccountCreated() {
        accountsCreated.increment();
    }

    public void recordAccountClosed() {
        accountsClosed.increment();
    }

    // Loan metrics
    public void recordLoanApplication(String outcome) {
        loanApplicationsTotal.increment();
        if ("approved".equalsIgnoreCase(outcome)) {
            loanApplicationsApproved.increment();
        } else if ("rejected".equalsIgnoreCase(outcome)) {
            loanApplicationsRejected.increment();
        }
    }

    // Auth metrics
    public void recordLoginAttempt(boolean success) {
        loginAttempts.increment();
        if (success) {
            loginSuccesses.increment();
        } else {
            loginFailures.increment();
        }
    }

    // Update gauge values
    public void updateGauge(String name, long value) {
        AtomicLong gauge = gaugeValues.get(name);
        if (gauge != null) {
            gauge.set(value);
        }
    }

    // Get current metrics summary
    public MetricsSummary getMetricsSummary() {
        return new MetricsSummary(
                (long) transactionsTotal.count(),
                (long) transactionsApproved.count(),
                (long) transactionsDeclined.count(),
                calculateApprovalRate(transactionsApproved.count(), transactionsTotal.count()),
                (long) loanApplicationsTotal.count(),
                (long) loanApplicationsApproved.count(),
                calculateApprovalRate(loanApplicationsApproved.count(), loanApplicationsTotal.count()),
                (long) loginAttempts.count(),
                (long) loginSuccesses.count(),
                calculateSuccessRate(loginSuccesses.count(), loginAttempts.count())
        );
    }

    private double calculateApprovalRate(double approved, double total) {
        return total > 0 ? (approved / total) * 100 : 0;
    }

    private double calculateSuccessRate(double successes, double total) {
        return total > 0 ? (successes / total) * 100 : 0;
    }

    public record MetricsSummary(
            long totalTransactions,
            long approvedTransactions,
            long declinedTransactions,
            double transactionApprovalRate,
            long totalLoanApplications,
            long approvedLoans,
            double loanApprovalRate,
            long totalLoginAttempts,
            long successfulLogins,
            double loginSuccessRate
    ) {}
}
