package com.banking.identity.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Utility for masking PII (Personally Identifiable Information) in logs.
 */
@Component
public class PiiMaskingFilter {

    private static final Logger log = LoggerFactory.getLogger(PiiMaskingFilter.class);
    private static final String MASK = "***";
    private static final Set<String> PII_FIELDS = Set.of(
            "customerId", "accountNumber", "ssn", "email", "phone", "address"
    );

    private final ObjectMapper objectMapper;

    public PiiMaskingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Masks PII fields in a JSON string.
     */
    public String maskPii(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        try {
            JsonNode node = objectMapper.readTree(json);
            maskPiiFields(node);
            return objectMapper.writeValueAsString(node);
        } catch (IOException e) {
            log.warn("Failed to mask PII in JSON, returning original: {}", e.getMessage());
            return json;
        }
    }

    private void maskPiiFields(JsonNode node) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode value = entry.getValue();

                if (PII_FIELDS.contains(fieldName) && value.isTextual()) {
                    String originalValue = value.asText();
                    if (originalValue != null && !originalValue.isEmpty()) {
                        // Mask all but last 4 characters if length > 4
                        String masked = originalValue.length() > 4 
                                ? MASK + originalValue.substring(originalValue.length() - 4)
                                : MASK;
                        ((com.fasterxml.jackson.databind.node.ObjectNode) node).put(fieldName, masked);
                    }
                } else if (value.isObject() || value.isArray()) {
                    maskPiiFields(value);
                }
            });
        } else if (node.isArray()) {
            node.forEach(this::maskPiiFields);
        }
    }

    /**
     * Masks account number in a string (simple pattern matching).
     */
    public String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return MASK;
        }
        return MASK + accountNumber.substring(accountNumber.length() - 4);
    }

    /**
     * Masks customer ID (shows only last 4 characters of UUID).
     */
    public String maskCustomerId(String customerId) {
        if (customerId == null || customerId.length() <= 4) {
            return MASK;
        }
        return MASK + customerId.substring(customerId.length() - 4);
    }
}
