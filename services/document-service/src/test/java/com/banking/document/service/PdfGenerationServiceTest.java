package com.banking.document.service;

import com.banking.document.service.PdfGenerationService.StatementData;
import com.banking.document.service.PdfGenerationService.TransactionLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PdfGenerationServiceTest {

    private final PdfGenerationService pdfService = new PdfGenerationService();

    @Test
    void testGenerateAccountStatement() {
        List<TransactionLine> transactions = List.of(
            new TransactionLine(LocalDate.now().minusDays(5), "Direct Deposit", BigDecimal.valueOf(1000), BigDecimal.valueOf(6000)),
            new TransactionLine(LocalDate.now().minusDays(3), "ATM Withdrawal", BigDecimal.valueOf(-200), BigDecimal.valueOf(5800)),
            new TransactionLine(LocalDate.now().minusDays(1), "Bill Payment", BigDecimal.valueOf(-150), BigDecimal.valueOf(5650))
        );

        StatementData data = new StatementData(
            UUID.randomUUID(),
            "John Doe",
            "1234567890",
            "Savings",
            LocalDate.now().minusMonths(1),
            LocalDate.now(),
            BigDecimal.valueOf(5000),
            BigDecimal.valueOf(5650),
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(350),
            transactions
        );

        byte[] pdf = pdfService.generateAccountStatement(data);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        // PDF files start with %PDF
        assertEquals(0x25, pdf[0]); // %
        assertEquals(0x50, pdf[1]); // P
        assertEquals(0x44, pdf[2]); // D
        assertEquals(0x46, pdf[3]); // F
    }

    @Test
    void testGenerateEmptyStatement() {
        StatementData data = new StatementData(
            UUID.randomUUID(),
            "Jane Doe",
            "9876543210",
            "Checking",
            LocalDate.now().minusMonths(1),
            LocalDate.now(),
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(1000),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            List.of()
        );

        byte[] pdf = pdfService.generateAccountStatement(data);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }
}
