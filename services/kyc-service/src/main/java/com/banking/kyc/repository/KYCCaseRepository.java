package com.banking.kyc.repository;

import com.banking.kyc.domain.KYCStatus;
import com.banking.kyc.domain.KYCCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KYCCaseRepository extends JpaRepository<KYCCase, UUID> {

    Optional<KYCCase> findByCustomerIdAndStatusNotIn(UUID customerId, List<KYCStatus> excludedStatuses);

    Page<KYCCase> findByCustomerId(UUID customerId, Pageable pageable);

    Page<KYCCase> findByStatus(KYCStatus status, Pageable pageable);

    Page<KYCCase> findByAssignedTo(UUID assignedTo, Pageable pageable);

    @Query("SELECT k FROM KYCCase k WHERE k.status = :status AND k.dueDate < :now")
    List<KYCCase> findOverdueCases(@Param("status") KYCStatus status, @Param("now") java.time.Instant now);

    @Query("SELECT COUNT(k) FROM KYCCase k WHERE k.status = :status")
    long countByStatus(@Param("status") KYCStatus status);
}

