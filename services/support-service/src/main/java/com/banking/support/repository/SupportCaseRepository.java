package com.banking.support.repository;

import com.banking.support.domain.CasePriority;
import com.banking.support.domain.CaseStatus;
import com.banking.support.domain.CaseType;
import com.banking.support.domain.SupportCase;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportCaseRepository extends JpaRepository<SupportCase, UUID> {

    Optional<SupportCase> findByCaseNumber(String caseNumber);

    Page<SupportCase> findByStatus(CaseStatus status, Pageable pageable);

    Page<SupportCase> findByPriority(CasePriority priority, Pageable pageable);

    Page<SupportCase> findByCaseType(CaseType caseType, Pageable pageable);

    Page<SupportCase> findByCustomerId(UUID customerId, Pageable pageable);

    Page<SupportCase> findByAssignedTo(UUID assignedTo, Pageable pageable);

    @Query("SELECT c FROM SupportCase c WHERE c.status IN :statuses")
    Page<SupportCase> findByStatusIn(@Param("statuses") java.util.List<CaseStatus> statuses, Pageable pageable);
}

