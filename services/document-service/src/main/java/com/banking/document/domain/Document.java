package com.banking.document.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "document_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(name = "category", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DocumentCategory category;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum DocumentType {
        KYC_ID_PROOF,
        KYC_ADDRESS_PROOF,
        KYC_PHOTO,
        ACCOUNT_STATEMENT,
        LOAN_AGREEMENT,
        CARD_AGREEMENT,
        TAX_DOCUMENT,
        SIGNATURE,
        OTHER
    }

    public enum DocumentCategory {
        KYC,
        STATEMENT,
        CONTRACT,
        TAX,
        CORRESPONDENCE,
        OTHER
    }

    public enum DocumentStatus {
        PENDING,
        VERIFIED,
        REJECTED,
        EXPIRED,
        DELETED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        if (status == null) {
            status = DocumentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }

    public DocumentCategory getCategory() { return category; }
    public void setCategory(DocumentCategory category) { this.category = category; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
