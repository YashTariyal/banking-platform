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
public class InterestCalculationJob {

    private static final Logger log = LoggerFactory.getLogger(InterestCalculationJob.class);

    private final ScheduledJobRepository jobRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean enabled;

    public InterestCalculationJob(
            ScheduledJobRepository jobRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${scheduler.interest-calculation.enabled:true}") boolean enabled) {
        this.jobRepository = jobRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${scheduler.interest-calculation.cron}")
    @Transactional
    public void execute() {
        if (!enabled) {
            log.info("Interest calculation job is disabled");
            return;
        }
        runJob();
    }

    @Transactional
    public ScheduledJob runJob() {
        log.info("Starting interest calculation job");

        ScheduledJob job = new ScheduledJob();
        job.setJobName("Daily Interest Calculation");
        job.setJobType(JobType.INTEREST_CALCULATION);
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job = jobRepository.save(job);

        try {
            // Publish event to trigger interest calculation in account-service
            Map<String, Object> event = Map.of(
                "eventType", "INTEREST_CALCULATION_TRIGGERED",
                "jobId", job.getId().toString(),
                "timestamp", Instant.now().toString()
            );
            kafkaTemplate.send("scheduler-events", job.getId().toString(), event);

            // Simulate processing (in real impl, this would wait for completion events)
            long recordsProcessed = simulateInterestCalculation();

            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            job.setRecordsProcessed(recordsProcessed);
            job.setRecordsFailed(0L);

            log.info("Interest calculation job completed. Processed {} records", recordsProcessed);

        } catch (Exception e) {
            log.error("Interest calculation job failed", e);
            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage(e.getMessage());
        }

        return jobRepository.save(job);
    }

    private long simulateInterestCalculation() {
        // In real implementation, this would:
        // 1. Fetch all active savings/interest-bearing accounts
        // 2. Calculate daily interest based on balance and rate
        // 3. Post interest accrual entries to ledger
        // 4. Return count of processed accounts
        return 1000L; // Simulated count
    }
}
