package com.banking.account.service;

public class InvalidIdempotencyKeyException extends RuntimeException {

    public InvalidIdempotencyKeyException(String message) {
        super(message);
    }

    public static InvalidIdempotencyKeyException nullKey() {
        return new InvalidIdempotencyKeyException("Idempotency key (referenceId) cannot be null");
    }

    public static InvalidIdempotencyKeyException invalidFormat() {
        return new InvalidIdempotencyKeyException("Idempotency key (referenceId) must be a valid UUID");
    }
}

