package com.banking.kyc.web;

import com.banking.kyc.domain.Document;
import com.banking.kyc.domain.DocumentType;
import com.banking.kyc.service.DocumentService;
import com.banking.kyc.web.dto.CreateDocumentRequest;
import com.banking.kyc.web.dto.DocumentResponse;
import com.banking.kyc.web.dto.VerifyDocumentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kyc/documents")
@Tag(name = "Documents", description = "Document management and verification")
public class DocumentController {

    private final DocumentService documentService;
    private final KYCMapper mapper;

    public DocumentController(DocumentService documentService, KYCMapper mapper) {
        this.documentService = documentService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Upload a document",
            description = "Uploads a document for a KYC case"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Document uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "KYC case not found")
    })
    public ResponseEntity<DocumentResponse> uploadDocument(@Valid @RequestBody CreateDocumentRequest request) {
        Document document = documentService.uploadDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(document));
    }

    @GetMapping("/case/{kycCaseId}")
    @Operation(
            summary = "Get documents for a KYC case",
            description = "Retrieves all documents for a specific KYC case"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documents retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "KYC case not found")
    })
    public ResponseEntity<List<DocumentResponse>> getDocumentsByCase(
            @Parameter(description = "KYC case unique identifier", required = true)
            @PathVariable UUID kycCaseId,
            @Parameter(description = "Filter by document type (optional)")
            @RequestParam(required = false) DocumentType documentType
    ) {
        List<Document> documents = documentType != null
                ? documentService.getDocumentsByType(kycCaseId, documentType)
                : documentService.getDocumentsByCase(kycCaseId);
        return ResponseEntity.ok(documents.stream().map(mapper::toResponse).toList());
    }

    @PutMapping("/{id}/verify")
    @Operation(
            summary = "Verify a document",
            description = "Verifies or rejects a document. Automatically updates KYC case status when all documents are verified."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document verified successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<DocumentResponse> verifyDocument(
            @PathVariable UUID id,
            @Valid @RequestBody VerifyDocumentRequest request
    ) {
        Document document = documentService.verifyDocument(id, request);
        return ResponseEntity.ok(mapper.toResponse(document));
    }
}

