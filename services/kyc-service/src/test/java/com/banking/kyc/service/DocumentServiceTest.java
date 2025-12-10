package com.banking.kyc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.kyc.domain.Document;
import com.banking.kyc.domain.DocumentType;
import com.banking.kyc.domain.KYCCase;
import com.banking.kyc.domain.VerificationStatus;
import com.banking.kyc.repository.DocumentRepository;
import com.banking.kyc.repository.KYCCaseRepository;
import com.banking.kyc.web.dto.CreateDocumentRequest;
import com.banking.kyc.web.dto.VerifyDocumentRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private KYCCaseRepository kycCaseRepository;

    @Mock
    private KYCService kycService;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(documentRepository, kycCaseRepository, kycService);
    }

    @Test
    void uploadDocument_CreatesDocument() {
        // Given
        UUID kycCaseId = UUID.randomUUID();
        CreateDocumentRequest request = new CreateDocumentRequest(
                kycCaseId, DocumentType.PASSPORT, "passport.pdf", "/path/to/file", 1024L,
                "application/pdf", "P123456", "US", null
        );

        KYCCase kycCase = new KYCCase();
        kycCase.setId(kycCaseId);

        when(kycCaseRepository.findById(kycCaseId)).thenReturn(Optional.of(kycCase));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });

        // When
        Document result = documentService.uploadDocument(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDocumentType()).isEqualTo(DocumentType.PASSPORT);
        assertThat(result.getFileName()).isEqualTo("passport.pdf");
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void verifyDocument_VerifiesDocument() {
        // Given
        UUID documentId = UUID.randomUUID();
        UUID kycCaseId = UUID.randomUUID();
        Document document = createDocument();
        document.setId(documentId);
        document.setKycCaseId(kycCaseId);

        VerifyDocumentRequest request = new VerifyDocumentRequest(
                VerificationStatus.VERIFIED, UUID.randomUUID(), "Verified"
        );

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.countByKycCaseIdAndVerificationStatus(any(), any())).thenReturn(0L);
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Document verified = documentService.verifyDocument(documentId, request);

        // Then
        assertThat(verified.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(verified.getVerifiedAt()).isNotNull();
        verify(kycService).markDocumentVerificationCompleted(kycCaseId);
    }

    private Document createDocument() {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setKycCaseId(UUID.randomUUID());
        document.setDocumentType(DocumentType.PASSPORT);
        document.setFileName("test.pdf");
        document.setVerificationStatus(VerificationStatus.PENDING);
        return document;
    }
}

