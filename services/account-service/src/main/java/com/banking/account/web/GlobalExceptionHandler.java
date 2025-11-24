package com.banking.account.web;

import com.banking.account.service.AccountClosureException;
import com.banking.account.service.AccountGoalContributionException;
import com.banking.account.service.AccountGoalNotFoundException;
import com.banking.account.service.AccountGoalValidationException;
import com.banking.account.service.AccountLimitException;
import com.banking.account.service.AccountNotFoundException;
import com.banking.account.service.ConcurrentAccountUpdateException;
import com.banking.account.service.CustomerNotFoundException;
import com.banking.account.service.InvalidAccountStatusException;
import com.banking.account.service.InvalidAccountTypeException;
import com.banking.account.service.InvalidIdempotencyKeyException;
import com.banking.account.service.UnsupportedCurrencyException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(AccountNotFoundException exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                "Account not found",
                List.of(exception.getMessage())
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + " " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .toList();

        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                details
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Constraint violation",
                exception.getConstraintViolations()
                        .stream()
                        .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                        .toList()
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request",
                List.of(exception.getMessage())
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(ConcurrentAccountUpdateException.class)
    public ResponseEntity<ApiError> handleConcurrentUpdate(ConcurrentAccountUpdateException exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.CONFLICT.value(),
                "Account modified concurrently",
                List.of(exception.getMessage())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(InvalidAccountStatusException.class)
    public ResponseEntity<ApiError> handleInvalidAccountStatus(InvalidAccountStatusException exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid account status for operation",
                List.of(exception.getMessage())
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid operation",
                List.of(exception.getMessage())
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(AccountClosureException.class)
    public ResponseEntity<ApiError> handleAccountClosure(AccountClosureException exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Cannot close account",
                List.of(exception.getMessage())
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(AccountLimitException.class)
    public ResponseEntity<ApiError> handleAccountLimit(AccountLimitException exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Account limit violation",
                List.of(exception.getMessage())
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(AccountGoalNotFoundException.class)
    public ResponseEntity<ApiError> handleGoalNotFound(AccountGoalNotFoundException exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                "Goal not found",
                List.of(exception.getMessage())
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    @ExceptionHandler(AccountGoalValidationException.class)
    public ResponseEntity<ApiError> handleGoalValidation(AccountGoalValidationException exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid goal request",
                List.of(exception.getMessage())
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(AccountGoalContributionException.class)
    public ResponseEntity<ApiError> handleGoalContribution(AccountGoalContributionException exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Goal contribution failed",
                List.of(exception.getMessage())
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiError> handleCustomerNotFound(CustomerNotFoundException exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Customer not found",
                List.of(exception.getMessage())
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    public ResponseEntity<ApiError> handleInvalidIdempotencyKey(InvalidIdempotencyKeyException exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid idempotency key",
                List.of(exception.getMessage())
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(UnsupportedCurrencyException.class)
    public ResponseEntity<ApiError> handleUnsupportedCurrency(UnsupportedCurrencyException exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Unsupported currency",
                List.of(exception.getMessage())
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(InvalidAccountTypeException.class)
    public ResponseEntity<ApiError> handleInvalidAccountType(InvalidAccountTypeException exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid account type",
                List.of(exception.getMessage())
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception exception) {
        ApiError apiError = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Unexpected error",
                List.of(exception.getMessage())
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }
}

