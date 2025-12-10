package com.banking.compliance.repository;

import com.banking.compliance.domain.Severity;
import com.banking.compliance.domain.SuspiciousActivity;
import com.banking.compliance.domain.SuspiciousActivityStatus;
import com.banking.compliance.domain.SuspiciousActivityType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SuspiciousActivityRepository extends JpaRepository<SuspiciousActivity, UUID> {

    Page<SuspiciousActivity> findByCustomerId(UUID customerId, Pageable pageable);

    Page<SuspiciousActivity> findByStatus(SuspiciousActivityStatus status, Pageable pageable);

    Page<SuspiciousActivity> findBySeverity(Severity severity, Pageable pageable);

    Page<SuspiciousActivity> findByActivityType(SuspiciousActivityType activityType, Pageable pageable);

    Page<SuspiciousActivity> findByStatusAndSeverity(
            SuspiciousActivityStatus status,
            Severity severity,
            Pageable pageable
    );

    @Query("SELECT sa FROM SuspiciousActivity sa WHERE sa.status = 'OPEN' " +
           "ORDER BY sa.severity DESC, sa.createdAt DESC")
    List<SuspiciousActivity> findOpenActivitiesOrderedBySeverity(Pageable pageable);

    @Query("SELECT COUNT(sa) FROM SuspiciousActivity sa WHERE sa.customerId = :customerId " +
           "AND sa.status = :status")
    long countByCustomerIdAndStatus(@Param("customerId") UUID customerId, @Param("status") SuspiciousActivityStatus status);

    @Query("SELECT sa FROM SuspiciousActivity sa WHERE sa.createdAt >= :startDate " +
           "AND sa.createdAt <= :endDate")
    List<SuspiciousActivity> findByDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}

