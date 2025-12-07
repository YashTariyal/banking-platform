package com.banking.card.service;

public class CardOperationException extends RuntimeException {
    public CardOperationException(String message) {
        super(message);
    }

    public CardOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

