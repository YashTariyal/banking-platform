package com.banking.scheduler.web;

import com.banking.scheduler.domain.ScheduledJob;
import com.banking.scheduler.domain.ScheduledJob.JobStatus;
import com.banking.scheduler.domain.ScheduledJob.JobType;
import com.banking.scheduler.repository.ScheduledJobRepository;
import com.banking.scheduler.service.EODReconciliationJob;
import com.banking.scheduler.service.InterestCalculationJob;
import com.banking.scheduler.service.StatementGenerationJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/scheduler")
@Tag(name = "Scheduler", description = "Scheduled job management APIs")
public class SchedulerController {

    private final ScheduledJobRepository jobRepository;
    private final InterestCalculationJob interestCalculationJob;
    private final StatementGenerationJob statementGenerationJob;
    private final EODReconciliationJob eodReconciliationJob;

    public SchedulerController(
            ScheduledJobRepository jobRepository,
            InterestCalculationJob interestCalculationJob,
            StatementGenerationJob statementGenerationJob,
            EODReconciliationJob eodReconciliationJob) {
        this.jobRepository = jobRepository;
        this.interestCalculationJob = interestCalculationJob;
        this.statementGenerationJob = statementGenerationJob;
        this.eodReconciliationJob = eodReconciliationJob;
    }

    @GetMapping("/jobs")
    @Operation(summary = "List all scheduled jobs")
    public Page<ScheduledJob> listJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) JobType type,
            @RequestParam(required = false) JobStatus status) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (type != null) {
            return jobRepository.findByJobType(type, pageRequest);
        }
        if (status != null) {
            return jobRepository.findByStatus(status, pageRequest);
        }
        return jobRepository.findAll(pageRequest);
    }

    @GetMapping("/jobs/{id}")
    @Operation(summary = "Get job by ID")
    public ResponseEntity<ScheduledJob> getJob(@PathVariable UUID id) {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/jobs/interest-calculation/trigger")
    @Operation(summary = "Manually trigger interest calculation job")
    public ResponseEntity<ScheduledJob> triggerInterestCalculation() {
        ScheduledJob job = interestCalculationJob.runJob();
        return ResponseEntity.ok(job);
    }

    @PostMapping("/jobs/statement-generation/trigger")
    @Operation(summary = "Manually trigger statement generation job")
    public ResponseEntity<ScheduledJob> triggerStatementGeneration() {
        ScheduledJob job = statementGenerationJob.runJob();
        return ResponseEntity.ok(job);
    }

    @PostMapping("/jobs/eod-reconciliation/trigger")
    @Operation(summary = "Manually trigger EOD reconciliation job")
    public ResponseEntity<ScheduledJob> triggerEODReconciliation() {
        ScheduledJob job = eodReconciliationJob.runJob();
        return ResponseEntity.ok(job);
    }

    @PostMapping("/jobs/{id}/cancel")
    @Operation(summary = "Cancel a pending or running job")
    public ResponseEntity<ScheduledJob> cancelJob(@PathVariable UUID id) {
        return jobRepository.findById(id)
                .map(job -> {
                    if (job.getStatus() == JobStatus.PENDING || job.getStatus() == JobStatus.RUNNING) {
                        job.setStatus(JobStatus.CANCELLED);
                        return ResponseEntity.ok(jobRepository.save(job));
                    }
                    return ResponseEntity.badRequest().<ScheduledJob>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
