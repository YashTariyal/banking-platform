package com.banking.account.service;

public class UnsupportedCurrencyException extends RuntimeException {

    public UnsupportedCurrencyException(String message) {
        super(message);
    }
}

