package com.banking.scheduler.domain;

import com.banking.scheduler.domain.ScheduledJob.JobStatus;
import com.banking.scheduler.domain.ScheduledJob.JobType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScheduledJobTest {

    @Test
    void testJobCreation() {
        ScheduledJob job = new ScheduledJob();
        job.setJobName("Test Job");
        job.setJobType(JobType.INTEREST_CALCULATION);
        job.setStatus(JobStatus.PENDING);

        assertEquals("Test Job", job.getJobName());
        assertEquals(JobType.INTEREST_CALCULATION, job.getJobType());
        assertEquals(JobStatus.PENDING, job.getStatus());
    }

    @Test
    void testJobStatusTransitions() {
        ScheduledJob job = new ScheduledJob();
        job.setStatus(JobStatus.PENDING);
        assertEquals(JobStatus.PENDING, job.getStatus());

        job.setStatus(JobStatus.RUNNING);
        assertEquals(JobStatus.RUNNING, job.getStatus());

        job.setStatus(JobStatus.COMPLETED);
        assertEquals(JobStatus.COMPLETED, job.getStatus());
    }

    @Test
    void testJobTypes() {
        assertEquals(4, JobType.values().length);
        assertNotNull(JobType.valueOf("INTEREST_CALCULATION"));
        assertNotNull(JobType.valueOf("STATEMENT_GENERATION"));
        assertNotNull(JobType.valueOf("EOD_RECONCILIATION"));
        assertNotNull(JobType.valueOf("MANUAL"));
    }

    @Test
    void testRecordsProcessedTracking() {
        ScheduledJob job = new ScheduledJob();
        job.setRecordsProcessed(100L);
        job.setRecordsFailed(5L);

        assertEquals(100L, job.getRecordsProcessed());
        assertEquals(5L, job.getRecordsFailed());
    }
}
