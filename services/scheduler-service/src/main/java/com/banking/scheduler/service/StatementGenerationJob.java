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
public class StatementGenerationJob {

    private static final Logger log = LoggerFactory.getLogger(StatementGenerationJob.class);

    private final ScheduledJobRepository jobRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean enabled;

    public StatementGenerationJob(
            ScheduledJobRepository jobRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${scheduler.statement-generation.enabled:true}") boolean enabled) {
        this.jobRepository = jobRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${scheduler.statement-generation.cron}")
    @Transactional
    public void execute() {
        if (!enabled) {
            log.info("Statement generation job is disabled");
            return;
        }
        runJob();
    }

    @Transactional
    public ScheduledJob runJob() {
        log.info("Starting statement generation job");

        ScheduledJob job = new ScheduledJob();
        job.setJobName("Monthly Statement Generation");
        job.setJobType(JobType.STATEMENT_GENERATION);
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job = jobRepository.save(job);

        try {
            // Publish event to trigger statement generation in document-service
            Map<String, Object> event = Map.of(
                "eventType", "STATEMENT_GENERATION_TRIGGERED",
                "jobId", job.getId().toString(),
                "timestamp", Instant.now().toString()
            );
            kafkaTemplate.send("scheduler-events", job.getId().toString(), event);

            long recordsProcessed = simulateStatementGeneration();

            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            job.setRecordsProcessed(recordsProcessed);
            job.setRecordsFailed(0L);

            log.info("Statement generation job completed. Generated {} statements", recordsProcessed);

        } catch (Exception e) {
            log.error("Statement generation job failed", e);
            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage(e.getMessage());
        }

        return jobRepository.save(job);
    }

    private long simulateStatementGeneration() {
        // In real implementation, this would:
        // 1. Fetch all active accounts
        // 2. Generate PDF statements for each account
        // 3. Store statements in document-service
        // 4. Optionally email statements to customers
        return 500L;
    }
}
