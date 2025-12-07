package com.banking.card.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CardOperationExceptionTest {

    @Test
    void cardOperationException() {
        String message = "Operation failed";
        CardOperationException exception = new CardOperationException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    void cardOperationExceptionWithCause() {
        String message = "Operation failed";
        Throwable cause = new RuntimeException("Root cause");
        CardOperationException exception = new CardOperationException(message, cause);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void cardLimitExceededException() {
        BigDecimal requested = BigDecimal.valueOf(1000);
        BigDecimal available = BigDecimal.valueOf(500);
        String message = "Limit exceeded";
        
        CardLimitExceededException exception = new CardLimitExceededException(message, requested, available);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getRequestedAmount()).isEqualByComparingTo(requested);
        assertThat(exception.getAvailableLimit()).isEqualByComparingTo(available);
    }

    @Test
    void cardRestrictionViolationException() {
        String message = "Restriction violated";
        String restrictionType = "MERCHANT_CATEGORY";
        String restrictionValue = "5812";
        
        CardRestrictionViolationException exception = new CardRestrictionViolationException(
                message, restrictionType, restrictionValue);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getRestrictionType()).isEqualTo(restrictionType);
        assertThat(exception.getRestrictionValue()).isEqualTo(restrictionValue);
    }
}

