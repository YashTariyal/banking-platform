package com.banking.scheduler.service;

import com.banking.scheduler.domain.ScheduledJob;
import com.banking.scheduler.domain.ScheduledJob.JobStatus;
import com.banking.scheduler.domain.ScheduledJob.JobType;
import com.banking.scheduler.repository.ScheduledJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class EODReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(EODReconciliationJob.class);

    private final ScheduledJobRepository jobRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean enabled;

    public EODReconciliationJob(
            ScheduledJobRepository jobRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${scheduler.eod-reconciliation.enabled:true}") boolean enabled) {
        this.jobRepository = jobRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${scheduler.eod-reconciliation.cron}")
    @Transactional
    public void execute() {
        if (!enabled) {
            log.info("EOD reconciliation job is disabled");
            return;
        }
        runJob();
    }

    @Transactional
    public ScheduledJob runJob() {
        log.info("Starting EOD reconciliation job");

        ScheduledJob job = new ScheduledJob();
        job.setJobName("End of Day Reconciliation");
        job.setJobType(JobType.EOD_RECONCILIATION);
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job = jobRepository.save(job);

        try {
            // Publish event to trigger reconciliation across services
            Map<String, Object> event = Map.of(
                "eventType", "EOD_RECONCILIATION_TRIGGERED",
                "jobId", job.getId().toString(),
                "timestamp", Instant.now().toString()
            );
            kafkaTemplate.send("scheduler-events", job.getId().toString(), event);

            long recordsProcessed = simulateReconciliation();

            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            job.setRecordsProcessed(recordsProcessed);
            job.setRecordsFailed(0L);

            log.info("EOD reconciliation job completed. Reconciled {} records", recordsProcessed);

        } catch (Exception e) {
            log.error("EOD reconciliation job failed", e);
            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage(e.getMessage());
        }

        return jobRepository.save(job);
    }

    private long simulateReconciliation() {
        // In real implementation, this would:
        // 1. Validate all ledger entries balance (debits = credits)
        // 2. Reconcile external payment gateway transactions
        // 3. Identify and flag discrepancies
        // 4. Generate reconciliation report
        return 2500L;
    }
}
