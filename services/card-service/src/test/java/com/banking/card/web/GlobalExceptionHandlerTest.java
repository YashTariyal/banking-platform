package com.banking.card.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.card.service.CardLimitExceededException;
import com.banking.card.service.CardNotFoundException;
import com.banking.card.service.CardOperationException;
import com.banking.card.service.CardRestrictionViolationException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFound() {
        UUID cardId = UUID.randomUUID();
        CardNotFoundException exception = new CardNotFoundException(cardId);
        
        ResponseEntity<GlobalExceptionHandler.ApiError> response = 
                exceptionHandler.handleNotFound(exception);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Card not found");
    }

    @Test
    void handleCardOperation() {
        CardOperationException exception = new CardOperationException("Operation failed");
        
        ResponseEntity<GlobalExceptionHandler.ApiError> response = 
                exceptionHandler.handleCardOperation(exception);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Card operation failed");
    }

    @Test
    void handleLimitExceeded() {
        CardLimitExceededException exception = new CardLimitExceededException(
                "Limit exceeded", 
                BigDecimal.valueOf(1000), 
                BigDecimal.valueOf(500));
        
        ResponseEntity<GlobalExceptionHandler.ApiError> response = 
                exceptionHandler.handleLimitExceeded(exception);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Limit exceeded");
        assertThat(response.getBody().details()).hasSize(3);
    }

    @Test
    void handleRestrictionViolation() {
        CardRestrictionViolationException exception = new CardRestrictionViolationException(
                "Restriction violated", 
                "MERCHANT_CATEGORY", 
                "5812");
        
        ResponseEntity<GlobalExceptionHandler.ApiError> response = 
                exceptionHandler.handleRestrictionViolation(exception);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().message()).isEqualTo("Restriction violated");
        assertThat(response.getBody().details()).hasSize(3);
    }

    @Test
    void handleIllegalArgument() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");
        
        ResponseEntity<GlobalExceptionHandler.ApiError> response = 
                exceptionHandler.handleIllegalArgument(exception);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
    }

    @Test
    void handleIllegalState() {
        IllegalStateException exception = new IllegalStateException("Invalid state");
        
        ResponseEntity<GlobalExceptionHandler.ApiError> response = 
                exceptionHandler.handleIllegalState(exception);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
    }
}

