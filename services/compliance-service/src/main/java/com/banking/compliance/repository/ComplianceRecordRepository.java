package com.banking.compliance.repository;

import com.banking.compliance.domain.ComplianceRecord;
import com.banking.compliance.domain.ComplianceRecordType;
import com.banking.compliance.domain.ComplianceStatus;
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
public interface ComplianceRecordRepository extends JpaRepository<ComplianceRecord, UUID> {

    Page<ComplianceRecord> findByCustomerId(UUID customerId, Pageable pageable);

    Page<ComplianceRecord> findByAccountId(UUID accountId, Pageable pageable);

    Page<ComplianceRecord> findByRecordType(ComplianceRecordType recordType, Pageable pageable);

    Page<ComplianceRecord> findByStatus(ComplianceStatus status, Pageable pageable);

    Page<ComplianceRecord> findByCustomerIdAndStatus(UUID customerId, ComplianceStatus status, Pageable pageable);

    @Query("SELECT cr FROM ComplianceRecord cr WHERE cr.customerId = :customerId " +
           "AND cr.createdAt >= :startDate AND cr.createdAt <= :endDate")
    List<ComplianceRecord> findByCustomerIdAndDateRange(
            @Param("customerId") UUID customerId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    @Query("SELECT COUNT(cr) FROM ComplianceRecord cr WHERE cr.customerId = :customerId " +
           "AND cr.status = :status")
    long countByCustomerIdAndStatus(@Param("customerId") UUID customerId, @Param("status") ComplianceStatus status);
}

