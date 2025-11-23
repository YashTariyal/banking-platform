package com.banking.account.service;

import java.util.UUID;

public class ConcurrentAccountUpdateException extends RuntimeException {

    public ConcurrentAccountUpdateException(UUID accountId, Throwable cause) {
        super("Account " + accountId + " was updated concurrently. Please retry.", cause);
    }
}

