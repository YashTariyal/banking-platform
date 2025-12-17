package com.banking.document.web;

import com.banking.document.domain.Document;
import com.banking.document.domain.Document.DocumentCategory;
import com.banking.document.service.DocumentService;
import com.banking.document.web.dto.DocumentResponse;
import com.banking.document.web.dto.UploadRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document management APIs")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("customerId") UUID customerId,
            @RequestParam(value = "accountId", required = false) UUID accountId,
            @RequestParam("documentType") Document.DocumentType documentType,
            @RequestParam("category") DocumentCategory category,
            @RequestParam(value = "description", required = false) String description) throws IOException {

        UploadRequest request = new UploadRequest(customerId, accountId, documentType, category, description, null);
        Document doc = documentService.uploadDocument(file, request);
        return ResponseEntity.ok(DocumentService.toResponse(doc));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document metadata")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable UUID id) {
        Document doc = documentService.getDocument(id);
        return ResponseEntity.ok(DocumentService.toResponse(doc));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download document content")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable UUID id) throws IOException {
        Document doc = documentService.getDocument(id);
        byte[] content = documentService.downloadDocument(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getOriginalFileName() + "\"")
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .contentLength(content.length)
                .body(content);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all documents for a customer")
    public ResponseEntity<List<DocumentResponse>> getCustomerDocuments(@PathVariable UUID customerId) {
        List<Document> docs = documentService.getCustomerDocuments(customerId);
        return ResponseEntity.ok(docs.stream().map(DocumentService::toResponse).toList());
    }

    @GetMapping("/customer/{customerId}/category/{category}")
    @Operation(summary = "Get customer documents by category")
    public ResponseEntity<List<DocumentResponse>> getCustomerDocumentsByCategory(
            @PathVariable UUID customerId,
            @PathVariable DocumentCategory category) {
        List<Document> docs = documentService.getCustomerDocumentsByCategory(customerId, category);
        return ResponseEntity.ok(docs.stream().map(DocumentService::toResponse).toList());
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get all documents for an account")
    public ResponseEntity<List<DocumentResponse>> getAccountDocuments(@PathVariable UUID accountId) {
        List<Document> docs = documentService.getAccountDocuments(accountId);
        return ResponseEntity.ok(docs.stream().map(DocumentService::toResponse).toList());
    }

    @PutMapping("/{id}/verify")
    @Operation(summary = "Verify a document")
    public ResponseEntity<DocumentResponse> verifyDocument(@PathVariable UUID id) {
        Document doc = documentService.verifyDocument(id, null);
        return ResponseEntity.ok(DocumentService.toResponse(doc));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject a document")
    public ResponseEntity<DocumentResponse> rejectDocument(
            @PathVariable UUID id,
            @RequestParam String reason) {
        Document doc = documentService.rejectDocument(id, reason, null);
        return ResponseEntity.ok(DocumentService.toResponse(doc));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) throws IOException {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/customer/{customerId}/kyc/count")
    @Operation(summary = "Count verified KYC documents for a customer")
    public ResponseEntity<Long> countVerifiedKycDocuments(@PathVariable UUID customerId) {
        return ResponseEntity.ok(documentService.countVerifiedKycDocuments(customerId));
    }
}
