package com.banking.kyc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PiiMaskingFilterTest {

    private PiiMaskingFilter piiMaskingFilter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        piiMaskingFilter = new PiiMaskingFilter(objectMapper);
    }

    @Test
    void maskPii_masksCustomerId() throws Exception {
        String json = "{\"customerId\":\"1234567890\",\"name\":\"John Doe\"}";
        String masked = piiMaskingFilter.maskPii(json);

        assertNotNull(masked);
        assertTrue(masked.contains("***7890"));
        assertFalse(masked.contains("1234567890"));
    }

    @Test
    void maskPii_masksEmail() throws Exception {
        String json = "{\"email\":\"test@example.com\",\"name\":\"John Doe\"}";
        String masked = piiMaskingFilter.maskPii(json);

        assertNotNull(masked);
        assertTrue(masked.contains("***"));
        assertFalse(masked.contains("test@example.com"));
    }

    @Test
    void maskAccountNumber_masksCorrectly() {
        String accountNumber = "1234567890";
        String masked = piiMaskingFilter.maskAccountNumber(accountNumber);

        assertEquals("***7890", masked);
    }

    @Test
    void maskCustomerId_masksCorrectly() {
        String customerId = "550e8400-e29b-41d4-a716-446655440000";
        String masked = piiMaskingFilter.maskCustomerId(customerId);

        assertTrue(masked.startsWith("***"));
        assertTrue(masked.endsWith("0000"));
    }

    @Test
    void maskPii_withNull_returnsNull() {
        String result = piiMaskingFilter.maskPii(null);
        assertNull(result);
    }

    @Test
    void maskPii_withEmptyString_returnsEmpty() {
        String result = piiMaskingFilter.maskPii("");
        assertEquals("", result);
    }
}
