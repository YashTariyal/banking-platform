package com.banking.scheduler.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scheduled_jobs")
public class ScheduledJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "job_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private JobType jobType;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "records_processed")
    private Long recordsProcessed;

    @Column(name = "records_failed")
    private Long recordsFailed;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum JobType {
        INTEREST_CALCULATION,
        STATEMENT_GENERATION,
        EOD_RECONCILIATION,
        MANUAL
    }

    public enum JobStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        if (status == null) {
            status = JobStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public JobType getJobType() { return jobType; }
    public void setJobType(JobType jobType) { this.jobType = jobType; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Long getRecordsProcessed() { return recordsProcessed; }
    public void setRecordsProcessed(Long recordsProcessed) { this.recordsProcessed = recordsProcessed; }

    public Long getRecordsFailed() { return recordsFailed; }
    public void setRecordsFailed(Long recordsFailed) { this.recordsFailed = recordsFailed; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
