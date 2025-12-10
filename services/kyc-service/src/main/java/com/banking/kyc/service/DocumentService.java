package com.banking.kyc.service;

import com.banking.kyc.domain.Document;
import com.banking.kyc.domain.DocumentType;
import com.banking.kyc.domain.VerificationStatus;
import com.banking.kyc.repository.DocumentRepository;
import com.banking.kyc.repository.KYCCaseRepository;
import com.banking.kyc.web.dto.CreateDocumentRequest;
import com.banking.kyc.web.dto.VerifyDocumentRequest;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final KYCCaseRepository kycCaseRepository;
    private final KYCService kycService;

    public DocumentService(
            DocumentRepository documentRepository,
            KYCCaseRepository kycCaseRepository,
            KYCService kycService
    ) {
        this.documentRepository = documentRepository;
        this.kycCaseRepository = kycCaseRepository;
        this.kycService = kycService;
    }

    public Document uploadDocument(CreateDocumentRequest request) {
        // Validate KYC case exists
        kycCaseRepository.findById(request.kycCaseId())
                .orElseThrow(() -> new IllegalArgumentException("KYC case not found: " + request.kycCaseId()));

        Document document = new Document();
        document.setKycCaseId(request.kycCaseId());
        document.setDocumentType(request.documentType());
        document.setFileName(request.fileName());
        document.setFilePath(request.filePath());
        document.setFileSize(request.fileSize());
        document.setMimeType(request.mimeType());
        document.setDocumentNumber(request.documentNumber());
        document.setIssuingCountry(request.issuingCountry());
        document.setExpiryDate(request.expiryDate());
        document.setVerificationStatus(VerificationStatus.PENDING);

        return documentRepository.save(document);
    }

    public List<Document> getDocumentsByCase(UUID kycCaseId) {
        kycCaseRepository.findById(kycCaseId)
                .orElseThrow(() -> new IllegalArgumentException("KYC case not found: " + kycCaseId));
        return documentRepository.findByKycCaseId(kycCaseId);
    }

    public Document verifyDocument(UUID documentId, VerifyDocumentRequest request) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        document.setVerificationStatus(request.verificationStatus());
        document.setVerifiedAt(Instant.now());
        document.setVerifiedBy(request.verifiedBy());
        document.setVerificationNotes(request.verificationNotes());

        Document verified = documentRepository.save(document);

        // Check if all documents are verified
        long pendingCount = documentRepository.countByKycCaseIdAndVerificationStatus(
                document.getKycCaseId(), VerificationStatus.PENDING
        );
        if (pendingCount == 0) {
            kycService.markDocumentVerificationCompleted(document.getKycCaseId());
        }

        return verified;
    }

    public List<Document> getDocumentsByType(UUID kycCaseId, DocumentType documentType) {
        return documentRepository.findByKycCaseIdAndDocumentType(kycCaseId, documentType);
    }
}

