package com.banking.customer.config;

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
    void maskAccountNumber_masksCorrectly() {
        String accountNumber = "1234567890";
        String masked = piiMaskingFilter.maskAccountNumber(accountNumber);

        assertEquals("***7890", masked);
    }
}
