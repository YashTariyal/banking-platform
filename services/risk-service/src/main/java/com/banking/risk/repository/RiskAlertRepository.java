package com.banking.risk.repository;

import com.banking.risk.domain.AlertStatus;
import com.banking.risk.domain.RiskAlert;
import com.banking.risk.domain.RiskLevel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiskAlertRepository extends JpaRepository<RiskAlert, UUID> {

    Page<RiskAlert> findByStatus(AlertStatus status, Pageable pageable);

    Page<RiskAlert> findByRiskLevel(RiskLevel riskLevel, Pageable pageable);

    Page<RiskAlert> findByCustomerId(UUID customerId, Pageable pageable);

    Page<RiskAlert> findByAccountId(UUID accountId, Pageable pageable);

    @Query("SELECT a FROM RiskAlert a WHERE a.status IN :statuses")
    Page<RiskAlert> findByStatusIn(@Param("statuses") List<AlertStatus> statuses, Pageable pageable);

    @Query("SELECT a FROM RiskAlert a WHERE a.riskScore >= :minScore")
    Page<RiskAlert> findByRiskScoreGreaterThanEqual(@Param("minScore") Integer minScore, Pageable pageable);
}

