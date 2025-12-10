package com.banking.kyc.repository;

import com.banking.kyc.domain.Document;
import com.banking.kyc.domain.DocumentType;
import com.banking.kyc.domain.VerificationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByKycCaseId(UUID kycCaseId);

    List<Document> findByKycCaseIdAndDocumentType(UUID kycCaseId, DocumentType documentType);

    List<Document> findByKycCaseIdAndVerificationStatus(UUID kycCaseId, VerificationStatus status);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.kycCaseId = :kycCaseId AND d.verificationStatus = :status")
    long countByKycCaseIdAndVerificationStatus(@Param("kycCaseId") UUID kycCaseId, @Param("status") VerificationStatus status);
}

