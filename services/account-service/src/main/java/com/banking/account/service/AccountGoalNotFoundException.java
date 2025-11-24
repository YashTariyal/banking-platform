package com.banking.account.service;

import java.util.UUID;

public class AccountGoalNotFoundException extends RuntimeException {

    public AccountGoalNotFoundException(UUID goalId) {
        super("Account goal not found: " + goalId);
    }
}

