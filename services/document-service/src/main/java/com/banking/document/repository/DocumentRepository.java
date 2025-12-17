package com.banking.document.repository;

import com.banking.document.domain.Document;
import com.banking.document.domain.Document.DocumentCategory;
import com.banking.document.domain.Document.DocumentStatus;
import com.banking.document.domain.Document.DocumentType;
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
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @Query("SELECT d FROM Document d WHERE d.id = :id AND d.deletedAt IS NULL")
    Optional<Document> findActiveById(@Param("id") UUID id);

    @Query("SELECT d FROM Document d WHERE d.customerId = :customerId AND d.deletedAt IS NULL ORDER BY d.createdAt DESC")
    List<Document> findByCustomerId(@Param("customerId") UUID customerId);

    @Query("SELECT d FROM Document d WHERE d.accountId = :accountId AND d.deletedAt IS NULL ORDER BY d.createdAt DESC")
    List<Document> findByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT d FROM Document d WHERE d.customerId = :customerId AND d.category = :category AND d.deletedAt IS NULL")
    List<Document> findByCustomerIdAndCategory(@Param("customerId") UUID customerId, @Param("category") DocumentCategory category);

    @Query("SELECT d FROM Document d WHERE d.customerId = :customerId AND d.documentType = :type AND d.deletedAt IS NULL")
    List<Document> findByCustomerIdAndType(@Param("customerId") UUID customerId, @Param("type") DocumentType type);

    @Query("SELECT d FROM Document d WHERE d.status = :status AND d.deletedAt IS NULL")
    Page<Document> findByStatus(@Param("status") DocumentStatus status, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.expiresAt <= :now AND d.status != 'EXPIRED' AND d.deletedAt IS NULL")
    List<Document> findExpiredDocuments(@Param("now") Instant now);

    @Query("SELECT d FROM Document d WHERE d.category = :category AND d.deletedAt IS NULL ORDER BY d.createdAt DESC")
    Page<Document> findByCategory(@Param("category") DocumentCategory category, Pageable pageable);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.customerId = :customerId AND d.category = 'KYC' AND d.status = 'VERIFIED' AND d.deletedAt IS NULL")
    long countVerifiedKycDocuments(@Param("customerId") UUID customerId);
}
