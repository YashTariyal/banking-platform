package com.banking.scheduler.repository;

import com.banking.scheduler.domain.ScheduledJob;
import com.banking.scheduler.domain.ScheduledJob.JobStatus;
import com.banking.scheduler.domain.ScheduledJob.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, UUID> {

    Page<ScheduledJob> findByJobType(JobType jobType, Pageable pageable);

    Page<ScheduledJob> findByStatus(JobStatus status, Pageable pageable);

    List<ScheduledJob> findByStatusIn(List<JobStatus> statuses);

    @Query("SELECT j FROM ScheduledJob j WHERE j.jobType = :jobType AND j.status = :status ORDER BY j.createdAt DESC")
    List<ScheduledJob> findByJobTypeAndStatus(@Param("jobType") JobType jobType, @Param("status") JobStatus status);

    @Query("SELECT j FROM ScheduledJob j WHERE j.createdAt >= :since ORDER BY j.createdAt DESC")
    List<ScheduledJob> findJobsSince(@Param("since") Instant since);

    Optional<ScheduledJob> findTopByJobTypeOrderByCreatedAtDesc(JobType jobType);

    @Query("SELECT COUNT(j) FROM ScheduledJob j WHERE j.jobType = :jobType AND j.status = :status AND j.createdAt >= :since")
    long countByJobTypeAndStatusSince(@Param("jobType") JobType jobType, @Param("status") JobStatus status, @Param("since") Instant since);
}
