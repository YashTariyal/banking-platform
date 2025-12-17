package com.banking.document.domain;

import com.banking.document.domain.Document.DocumentCategory;
import com.banking.document.domain.Document.DocumentStatus;
import com.banking.document.domain.Document.DocumentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTest {

    @Test
    void testDocumentCreation() {
        Document doc = new Document();
        doc.setCustomerId(UUID.randomUUID());
        doc.setDocumentType(DocumentType.KYC_ID_PROOF);
        doc.setCategory(DocumentCategory.KYC);
        doc.setFileName("test.pdf");
        doc.setOriginalFileName("ID_Proof.pdf");
        doc.setContentType("application/pdf");
        doc.setFileSize(1024L);
        doc.setStoragePath("/storage/test.pdf");

        assertNotNull(doc.getCustomerId());
        assertEquals(DocumentType.KYC_ID_PROOF, doc.getDocumentType());
        assertEquals(DocumentCategory.KYC, doc.getCategory());
        assertEquals("test.pdf", doc.getFileName());
        assertEquals(1024L, doc.getFileSize());
    }

    @Test
    void testDocumentTypes() {
        assertEquals(9, DocumentType.values().length);
        assertNotNull(DocumentType.valueOf("KYC_ID_PROOF"));
        assertNotNull(DocumentType.valueOf("KYC_ADDRESS_PROOF"));
        assertNotNull(DocumentType.valueOf("ACCOUNT_STATEMENT"));
        assertNotNull(DocumentType.valueOf("LOAN_AGREEMENT"));
    }

    @Test
    void testDocumentCategories() {
        assertEquals(6, DocumentCategory.values().length);
        assertNotNull(DocumentCategory.valueOf("KYC"));
        assertNotNull(DocumentCategory.valueOf("STATEMENT"));
        assertNotNull(DocumentCategory.valueOf("CONTRACT"));
    }

    @Test
    void testDocumentStatusTransitions() {
        Document doc = new Document();
        doc.setStatus(DocumentStatus.PENDING);
        assertEquals(DocumentStatus.PENDING, doc.getStatus());

        doc.setStatus(DocumentStatus.VERIFIED);
        assertEquals(DocumentStatus.VERIFIED, doc.getStatus());

        doc.setStatus(DocumentStatus.REJECTED);
        assertEquals(DocumentStatus.REJECTED, doc.getStatus());
    }

    @Test
    void testChecksumStorage() {
        Document doc = new Document();
        String checksum = "abc123def456";
        doc.setChecksum(checksum);
        assertEquals(checksum, doc.getChecksum());
    }
}
