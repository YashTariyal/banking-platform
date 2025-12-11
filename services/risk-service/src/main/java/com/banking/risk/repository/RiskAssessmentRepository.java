package com.banking.risk.repository;

import com.banking.risk.domain.RiskAssessment;
import com.banking.risk.domain.RiskLevel;
import com.banking.risk.domain.RiskType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    Page<RiskAssessment> findByRiskType(RiskType riskType, Pageable pageable);

    Page<RiskAssessment> findByRiskLevel(RiskLevel riskLevel, Pageable pageable);

    Page<RiskAssessment> findByCustomerId(UUID customerId, Pageable pageable);

    Page<RiskAssessment> findByAccountId(UUID accountId, Pageable pageable);

    @Query("SELECT r FROM RiskAssessment r WHERE r.entityId = :entityId AND r.riskType = :riskType ORDER BY r.assessedAt DESC")
    List<RiskAssessment> findByEntityIdAndRiskType(@Param("entityId") UUID entityId, @Param("riskType") RiskType riskType);

    @Query("SELECT r FROM RiskAssessment r WHERE r.riskScore >= :minScore")
    Page<RiskAssessment> findByRiskScoreGreaterThanEqual(@Param("minScore") Integer minScore, Pageable pageable);
}

