package com.banking.document.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class PdfGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PdfGenerationService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public byte[] generateAccountStatement(StatementData data) {
        log.info("Generating statement for account {}", data.accountNumber());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);

            // Header
            document.add(new Paragraph("BANK STATEMENT")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Banking Platform Inc.")
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            // Account info
            document.add(new Paragraph("Account Holder: " + data.customerName()));
            document.add(new Paragraph("Account Number: " + data.accountNumber()));
            document.add(new Paragraph("Account Type: " + data.accountType()));
            document.add(new Paragraph("Statement Period: " + data.periodStart().format(DATE_FORMAT) + " - " + data.periodEnd().format(DATE_FORMAT)));
            document.add(new Paragraph("Generated: " + LocalDate.now().format(DATE_FORMAT)));

            document.add(new Paragraph("\n"));

            // Balance summary
            document.add(new Paragraph("ACCOUNT SUMMARY").setBold());
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{3, 2}))
                    .setWidth(UnitValue.createPercentValue(60));
            summaryTable.addCell(new Cell().add(new Paragraph("Opening Balance:")));
            summaryTable.addCell(new Cell().add(new Paragraph(formatCurrency(data.openingBalance()))));
            summaryTable.addCell(new Cell().add(new Paragraph("Total Credits:")));
            summaryTable.addCell(new Cell().add(new Paragraph(formatCurrency(data.totalCredits()))));
            summaryTable.addCell(new Cell().add(new Paragraph("Total Debits:")));
            summaryTable.addCell(new Cell().add(new Paragraph(formatCurrency(data.totalDebits()))));
            summaryTable.addCell(new Cell().add(new Paragraph("Closing Balance:").setBold()));
            summaryTable.addCell(new Cell().add(new Paragraph(formatCurrency(data.closingBalance())).setBold()));
            document.add(summaryTable);

            document.add(new Paragraph("\n"));

            // Transaction table
            document.add(new Paragraph("TRANSACTION DETAILS").setBold());
            Table txTable = new Table(UnitValue.createPercentArray(new float[]{2, 4, 2, 2}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Header row
            txTable.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()));
            txTable.addHeaderCell(new Cell().add(new Paragraph("Description").setBold()));
            txTable.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()));
            txTable.addHeaderCell(new Cell().add(new Paragraph("Balance").setBold()));

            // Transaction rows
            for (TransactionLine tx : data.transactions()) {
                txTable.addCell(new Cell().add(new Paragraph(tx.date().format(DATE_FORMAT))));
                txTable.addCell(new Cell().add(new Paragraph(tx.description())));
                txTable.addCell(new Cell().add(new Paragraph(formatCurrency(tx.amount()))));
                txTable.addCell(new Cell().add(new Paragraph(formatCurrency(tx.balance()))));
            }

            document.add(txTable);

            // Footer
            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("This is a computer-generated statement and does not require a signature.")
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER));

            document.close();
            log.info("Statement generated successfully, size: {} bytes", baos.size());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF statement", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("$%,.2f", amount);
    }

    public record StatementData(
            UUID customerId,
            String customerName,
            String accountNumber,
            String accountType,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal openingBalance,
            BigDecimal closingBalance,
            BigDecimal totalCredits,
            BigDecimal totalDebits,
            List<TransactionLine> transactions
    ) {}

    public record TransactionLine(
            LocalDate date,
            String description,
            BigDecimal amount,
            BigDecimal balance
    ) {}
}
