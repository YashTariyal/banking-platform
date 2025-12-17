package com.banking.document.service;

import com.banking.document.domain.Document;
import com.banking.document.domain.Document.DocumentCategory;
import com.banking.document.domain.Document.DocumentStatus;
import com.banking.document.domain.Document.DocumentType;
import com.banking.document.repository.DocumentRepository;
import com.banking.document.web.dto.DocumentResponse;
import com.banking.document.web.dto.UploadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentStorageService storageService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final List<String> allowedTypes;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentStorageService storageService,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${document.allowed-types}") List<String> allowedTypes) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.kafkaTemplate = kafkaTemplate;
        this.allowedTypes = allowedTypes;
    }

    @Transactional
    public Document uploadDocument(MultipartFile file, UploadRequest request) throws IOException {
        // Validate content type
        if (!allowedTypes.contains(file.getContentType())) {
            throw new IllegalArgumentException("File type not allowed: " + file.getContentType());
        }

        // Store file
        DocumentStorageService.StorageResult result = storageService.store(file, request.customerId());

        // Create document record
        Document doc = new Document();
        doc.setCustomerId(request.customerId());
        doc.setAccountId(request.accountId());
        doc.setDocumentType(request.documentType());
        doc.setCategory(request.category());
        doc.setFileName(result.fileName());
        doc.setOriginalFileName(file.getOriginalFilename());
        doc.setContentType(file.getContentType());
        doc.setFileSize(result.fileSize());
        doc.setStoragePath(result.storagePath());
        doc.setChecksum(result.checksum());
        doc.setStatus(DocumentStatus.PENDING);
        doc.setDescription(request.description());
        doc.setCreatedBy(request.uploadedBy());

        doc = documentRepository.save(doc);

        // Publish event
        publishEvent("DOCUMENT_UPLOADED", doc);

        log.info("Document uploaded: {} for customer {}", doc.getId(), request.customerId());
        return doc;
    }

    public Document getDocument(UUID id) {
        return documentRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
    }

    public byte[] downloadDocument(UUID id) throws IOException {
        Document doc = getDocument(id);
        return storageService.retrieve(doc.getStoragePath());
    }

    public List<Document> getCustomerDocuments(UUID customerId) {
        return documentRepository.findByCustomerId(customerId);
    }

    public List<Document> getCustomerDocumentsByCategory(UUID customerId, DocumentCategory category) {
        return documentRepository.findByCustomerIdAndCategory(customerId, category);
    }

    public List<Document> getAccountDocuments(UUID accountId) {
        return documentRepository.findByAccountId(accountId);
    }

    @Transactional
    public Document verifyDocument(UUID id, UUID verifiedBy) {
        Document doc = getDocument(id);
        doc.setStatus(DocumentStatus.VERIFIED);
        doc = documentRepository.save(doc);

        publishEvent("DOCUMENT_VERIFIED", doc);
        log.info("Document verified: {}", id);
        return doc;
    }

    @Transactional
    public Document rejectDocument(UUID id, String reason, UUID rejectedBy) {
        Document doc = getDocument(id);
        doc.setStatus(DocumentStatus.REJECTED);
        doc.setDescription(reason);
        doc = documentRepository.save(doc);

        publishEvent("DOCUMENT_REJECTED", doc);
        log.info("Document rejected: {} - {}", id, reason);
        return doc;
    }

    @Transactional
    public void deleteDocument(UUID id) throws IOException {
        Document doc = getDocument(id);
        doc.setDeletedAt(Instant.now());
        doc.setStatus(DocumentStatus.DELETED);
        documentRepository.save(doc);

        // Optionally delete from storage
        // storageService.delete(doc.getStoragePath());

        publishEvent("DOCUMENT_DELETED", doc);
        log.info("Document deleted: {}", id);
    }

    public long countVerifiedKycDocuments(UUID customerId) {
        return documentRepository.countVerifiedKycDocuments(customerId);
    }

    private void publishEvent(String eventType, Document doc) {
        try {
            Map<String, Object> event = Map.of(
                "eventType", eventType,
                "documentId", doc.getId().toString(),
                "customerId", doc.getCustomerId() != null ? doc.getCustomerId().toString() : "",
                "documentType", doc.getDocumentType().name(),
                "category", doc.getCategory().name(),
                "status", doc.getStatus().name(),
                "timestamp", Instant.now().toString()
            );
            kafkaTemplate.send("document-events", doc.getId().toString(), event);
        } catch (Exception e) {
            log.warn("Failed to publish document event", e);
        }
    }

    public static DocumentResponse toResponse(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getCustomerId(),
                doc.getAccountId(),
                doc.getDocumentType(),
                doc.getCategory(),
                doc.getOriginalFileName(),
                doc.getContentType(),
                doc.getFileSize(),
                doc.getStatus(),
                doc.getDescription(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
