package com.banking.kyc.repository;

import com.banking.kyc.domain.ScreeningResult;
import com.banking.kyc.domain.ScreeningResultStatus;
import com.banking.kyc.domain.ScreeningType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreeningResultRepository extends JpaRepository<ScreeningResult, UUID> {

    List<ScreeningResult> findByKycCaseId(UUID kycCaseId);

    List<ScreeningResult> findByCustomerId(UUID customerId);

    List<ScreeningResult> findByKycCaseIdAndScreeningType(UUID kycCaseId, ScreeningType screeningType);

    List<ScreeningResult> findByKycCaseIdAndResult(UUID kycCaseId, ScreeningResultStatus result);

    @Query("SELECT COUNT(s) FROM ScreeningResult s WHERE s.kycCaseId = :kycCaseId AND s.result = :result")
    long countByKycCaseIdAndResult(@Param("kycCaseId") UUID kycCaseId, @Param("result") ScreeningResultStatus result);
}

