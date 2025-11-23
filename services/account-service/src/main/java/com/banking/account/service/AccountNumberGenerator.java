package com.banking.account.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class AccountNumberGenerator {

    private static final String PREFIX = "ACC";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        StringBuilder builder = new StringBuilder(PREFIX).append("-");
        for (int i = 0; i < 12; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }
}

