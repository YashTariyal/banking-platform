package com.banking.document.web;

import com.banking.document.domain.Document;
import com.banking.document.domain.Document.DocumentCategory;
import com.banking.document.domain.Document.DocumentStatus;
import com.banking.document.domain.Document.DocumentType;
import com.banking.document.repository.DocumentRepository;
import com.banking.document.service.DocumentStorageService;
import com.banking.document.service.PdfGenerationService;
import com.banking.document.service.PdfGenerationService.StatementData;
import com.banking.document.service.PdfGenerationService.TransactionLine;
import com.banking.document.web.dto.DocumentResponse;
import com.banking.document.web.dto.StatementRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/api/statements")
@Tag(name = "Statements", description = "Account statement generation APIs")
public class StatementController {

    private final PdfGenerationService pdfService;
    private final DocumentStorageService storageService;
    private final DocumentRepository documentRepository;

    public StatementController(
            PdfGenerationService pdfService,
            DocumentStorageService storageService,
            DocumentRepository documentRepository) {
        this.pdfService = pdfService;
        this.storageService = storageService;
        this.documentRepository = documentRepository;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate account statement PDF")
    public ResponseEntity<DocumentResponse> generateStatement(@Valid @RequestBody StatementRequest request) throws IOException {
        // Generate sample transactions (in real impl, fetch from transaction-service)
        List<TransactionLine> transactions = generateSampleTransactions(request.periodStart(), request.periodEnd());

        BigDecimal openingBalance = BigDecimal.valueOf(5000);
        BigDecimal totalCredits = transactions.stream()
                .filter(t -> t.amount().compareTo(BigDecimal.ZERO) > 0)
                .map(TransactionLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDebits = transactions.stream()
                .filter(t -> t.amount().compareTo(BigDecimal.ZERO) < 0)
                .map(TransactionLine::amount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal closingBalance = openingBalance.add(totalCredits).subtract(totalDebits);

        StatementData data = new StatementData(
                request.customerId(),
                request.customerName(),
                request.accountNumber(),
                request.accountType(),
                request.periodStart(),
                request.periodEnd(),
                openingBalance,
                closingBalance,
                totalCredits,
                totalDebits,
                transactions
        );

        // Generate PDF
        byte[] pdfContent = pdfService.generateAccountStatement(data);

        // Store the PDF
        String filename = String.format("statement_%s_%s_%s.pdf",
                request.accountNumber(),
                request.periodStart().format(DateTimeFormatter.ofPattern("yyyyMM")),
                request.periodEnd().format(DateTimeFormatter.ofPattern("yyyyMM")));

        Path storedPath = storageService.storeGeneratedPdf(pdfContent, request.customerId(), filename);

        // Create document record
        Document doc = new Document();
        doc.setCustomerId(request.customerId());
        doc.setAccountId(request.accountId());
        doc.setDocumentType(DocumentType.ACCOUNT_STATEMENT);
        doc.setCategory(DocumentCategory.STATEMENT);
        doc.setFileName(filename);
        doc.setOriginalFileName(filename);
        doc.setContentType("application/pdf");
        doc.setFileSize((long) pdfContent.length);
        doc.setStoragePath(storedPath.toString());
        doc.setStatus(DocumentStatus.VERIFIED);
        doc.setDescription("Account statement for " + request.periodStart() + " to " + request.periodEnd());

        doc = documentRepository.save(doc);

        return ResponseEntity.ok(new DocumentResponse(
                doc.getId(),
                doc.getCustomerId(),
                doc.getAccountId(),
                doc.getDocumentType(),
                doc.getCategory(),
                doc.getFileName(),
                doc.getContentType(),
                doc.getFileSize(),
                doc.getStatus(),
                doc.getDescription(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        ));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download generated statement")
    public ResponseEntity<byte[]> downloadStatement(@PathVariable UUID id) throws IOException {
        Document doc = documentRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("Statement not found"));

        byte[] content = storageService.retrieve(doc.getStoragePath());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(content.length)
                .body(content);
    }

    @PostMapping("/preview")
    @Operation(summary = "Generate statement PDF preview (not stored)")
    public ResponseEntity<byte[]> previewStatement(@Valid @RequestBody StatementRequest request) {
        List<TransactionLine> transactions = generateSampleTransactions(request.periodStart(), request.periodEnd());

        BigDecimal openingBalance = BigDecimal.valueOf(5000);
        BigDecimal closingBalance = transactions.isEmpty() ? openingBalance : transactions.get(transactions.size() - 1).balance();
        BigDecimal totalCredits = transactions.stream()
                .filter(t -> t.amount().compareTo(BigDecimal.ZERO) > 0)
                .map(TransactionLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDebits = transactions.stream()
                .filter(t -> t.amount().compareTo(BigDecimal.ZERO) < 0)
                .map(TransactionLine::amount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StatementData data = new StatementData(
                request.customerId(),
                request.customerName(),
                request.accountNumber(),
                request.accountType(),
                request.periodStart(),
                request.periodEnd(),
                openingBalance,
                closingBalance,
                totalCredits,
                totalDebits,
                transactions
        );

        byte[] pdf = pdfService.generateAccountStatement(data);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private List<TransactionLine> generateSampleTransactions(LocalDate start, LocalDate end) {
        // Generate sample transactions for demo purposes
        List<TransactionLine> transactions = new ArrayList<>();
        Random random = new Random();
        BigDecimal balance = BigDecimal.valueOf(5000);
        LocalDate current = start;

        String[] descriptions = {
                "Direct Deposit - Payroll",
                "ATM Withdrawal",
                "Online Transfer",
                "Bill Payment - Utilities",
                "POS Purchase - Grocery",
                "Interest Credit",
                "Service Fee",
                "Wire Transfer In",
                "Check Deposit",
                "Debit Card Purchase"
        };

        while (!current.isAfter(end)) {
            if (random.nextBoolean()) {
                BigDecimal amount = BigDecimal.valueOf(random.nextInt(500) - 200);
                balance = balance.add(amount);
                transactions.add(new TransactionLine(
                        current,
                        descriptions[random.nextInt(descriptions.length)],
                        amount,
                        balance
                ));
            }
            current = current.plusDays(1);
        }

        return transactions;
    }
}
