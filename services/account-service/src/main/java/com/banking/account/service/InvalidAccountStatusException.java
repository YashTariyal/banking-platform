package com.banking.account.service;

import java.util.UUID;

public class InvalidAccountStatusException extends RuntimeException {

    public InvalidAccountStatusException(UUID accountId, String message) {
        super("Account " + accountId + ": " + message);
    }
}

