package com.banking.account.metrics;

import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountTransactionType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class AccountMetrics {

    private final Counter accountsCreated;
    private final Counter accountsUpdated;
    private final Counter transactionsCredit;
    private final Counter transactionsDebit;
    private final DistributionSummary transactionAmounts;
    private final DistributionSummary transactionAmountsCredit;
    private final DistributionSummary transactionAmountsDebit;
    private final DistributionSummary accountBalances;
    private final Timer transactionProcessingTime;
    private final Counter apiErrors;
    private final AtomicLong totalAccounts = new AtomicLong(0);
    private final ConcurrentHashMap<AccountStatus, AtomicLong> accountStatusCounts = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public AccountMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        accountsCreated = Counter.builder("accounts.created")
                .description("Number of accounts created")
                .register(meterRegistry);
        accountsUpdated = Counter.builder("accounts.updated")
                .description("Number of accounts updated")
                .register(meterRegistry);
        transactionsCredit = Counter.builder("accounts.transactions.credit")
                .description("Number of credit transactions applied to accounts")
                .register(meterRegistry);
        transactionsDebit = Counter.builder("accounts.transactions.debit")
                .description("Number of debit transactions applied to accounts")
                .register(meterRegistry);
        
        // Transaction amount histogram with percentiles
        transactionAmounts = DistributionSummary.builder("accounts.transactions.amount")
                .description("Transaction amounts distribution")
                .baseUnit("currency")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .register(meterRegistry);
        
        // Transaction amounts by type (separate summaries)
        transactionAmountsCredit = DistributionSummary.builder("accounts.transactions.amount")
                .description("Credit transaction amounts distribution")
                .baseUnit("currency")
                .tag("type", "CREDIT")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        
        transactionAmountsDebit = DistributionSummary.builder("accounts.transactions.amount")
                .description("Debit transaction amounts distribution")
                .baseUnit("currency")
                .tag("type", "DEBIT")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        
        // Account balance distribution with percentiles
        accountBalances = DistributionSummary.builder("accounts.balance")
                .description("Account balances distribution")
                .baseUnit("currency")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .register(meterRegistry);

        // Transaction processing time
        transactionProcessingTime = Timer.builder("accounts.transactions.processing.time")
                .description("Time taken to process transactions")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .register(meterRegistry);

        // API error counter
        apiErrors = Counter.builder("http.server.errors")
                .description("HTTP server errors by status code")
                .register(meterRegistry);

        // Total accounts gauge
        Gauge.builder("accounts.total", totalAccounts, AtomicLong::get)
                .description("Total number of accounts")
                .register(meterRegistry);

        // Account status distribution gauges
        for (AccountStatus status : AccountStatus.values()) {
            AtomicLong count = new AtomicLong(0);
            accountStatusCounts.put(status, count);
            Gauge.builder("accounts.status", count, AtomicLong::get)
                    .tag("status", status.name())
                    .description("Number of accounts by status")
                    .register(meterRegistry);
        }
    }

    public void incrementCreated() {
        accountsCreated.increment();
        totalAccounts.incrementAndGet();
        accountStatusCounts.get(AccountStatus.ACTIVE).incrementAndGet();
    }

    public void incrementUpdated() {
        accountsUpdated.increment();
    }

    public void incrementCredit() {
        transactionsCredit.increment();
    }

    public void incrementDebit() {
        transactionsDebit.increment();
    }

    public void recordTransactionAmount(BigDecimal amount, AccountTransactionType type) {
        transactionAmounts.record(amount.doubleValue());
        if (type == AccountTransactionType.CREDIT) {
            transactionAmountsCredit.record(amount.doubleValue());
        } else {
            transactionAmountsDebit.record(amount.doubleValue());
        }
    }

    public void recordBalance(BigDecimal balance) {
        accountBalances.record(balance.doubleValue());
    }

    public Timer.Sample startTransactionTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordTransactionTime(Timer.Sample sample) {
        sample.stop(transactionProcessingTime);
    }

    public Timer.Sample startApiTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordApiTime(Timer.Sample sample, String method, String path, int status) {
        // Create or get timer with tags
        Timer timer = Timer.builder("http.server.requests")
                .tag("method", method)
                .tag("uri", sanitizePath(path))
                .tag("status", String.valueOf(status))
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .register(meterRegistry);
        sample.stop(timer);
    }

    private String sanitizePath(String path) {
        // Replace UUIDs in path with {id} for better metric aggregation
        return path.replaceAll("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "/{id}");
    }

    public void recordApiError(String method, String path, int status) {
        Counter errorCounter = Counter.builder("http.server.errors")
                .tag("method", method)
                .tag("uri", sanitizePath(path))
                .tag("status", String.valueOf(status))
                .register(meterRegistry);
        errorCounter.increment();
    }

    public void updateAccountStatus(AccountStatus oldStatus, AccountStatus newStatus) {
        if (oldStatus != null) {
            accountStatusCounts.get(oldStatus).decrementAndGet();
        }
        if (newStatus != null) {
            accountStatusCounts.get(newStatus).incrementAndGet();
        }
    }
}

