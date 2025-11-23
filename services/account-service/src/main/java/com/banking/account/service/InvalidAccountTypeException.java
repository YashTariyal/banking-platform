package com.banking.account.service;

import com.banking.account.domain.AccountType;

public class InvalidAccountTypeException extends RuntimeException {

    public InvalidAccountTypeException(String message) {
        super(message);
    }

    public static InvalidAccountTypeException invalidCombination(AccountType currentType, AccountType newType) {
        return new InvalidAccountTypeException(
                String.format("Cannot change account type from %s to %s. Invalid type combination.", 
                        currentType, newType));
    }

    public static InvalidAccountTypeException invalidTypeForCustomer(AccountType type) {
        return new InvalidAccountTypeException(
                String.format("Account type %s is not allowed for this customer", type));
    }
}

