package com.banking.card.web;

import com.banking.card.service.CardLimitExceededException;
import com.banking.card.service.CardNotFoundException;
import com.banking.card.service.CardOperationException;
import com.banking.card.service.CardRestrictionViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(CardNotFoundException ex) {
        ApiError error = new ApiError(HttpStatus.NOT_FOUND.value(), "Card not found", List.of(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(e -> e.getDefaultMessage())
                .toList();
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), "Validation failed", details);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), "Invalid request", List.of(ex.getMessage()));
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), "Invalid state", List.of(ex.getMessage()));
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(CardOperationException.class)
    public ResponseEntity<ApiError> handleCardOperation(CardOperationException ex) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), "Card operation failed", List.of(ex.getMessage()));
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(CardLimitExceededException.class)
    public ResponseEntity<ApiError> handleLimitExceeded(CardLimitExceededException ex) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), 
                "Limit exceeded", 
                List.of(ex.getMessage(), 
                        "Requested: " + ex.getRequestedAmount(), 
                        "Available: " + ex.getAvailableLimit()));
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(CardRestrictionViolationException.class)
    public ResponseEntity<ApiError> handleRestrictionViolation(CardRestrictionViolationException ex) {
        ApiError error = new ApiError(HttpStatus.FORBIDDEN.value(), 
                "Restriction violated", 
                List.of(ex.getMessage(), 
                        "Type: " + ex.getRestrictionType(), 
                        "Value: " + ex.getRestrictionValue()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    public record ApiError(
            int status,
            String message,
            List<String> details
    ) {
    }
}


