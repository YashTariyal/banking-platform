package com.banking.card.integration;

import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Stub implementation of BalanceServiceClient for development/testing.
 * In production, this would be replaced with a real Feign client or REST template.
 */
@Component
@ConditionalOnMissingBean(name = "balanceServiceClient")
public class StubBalanceServiceClient implements BalanceServiceClient {

    private static final Logger log = LoggerFactory.getLogger(StubBalanceServiceClient.class);

    @Override
    public boolean hasSufficientBalance(UUID accountId, BigDecimal amount, String currency) {
        log.debug("Stub: Checking balance for account {} - amount: {} {}", accountId, amount, currency);
        // Stub implementation - always returns true
        // In production, this would call the actual account service
        return true;
    }

    @Override
    public BigDecimal getAvailableBalance(UUID accountId, String currency) {
        log.debug("Stub: Getting balance for account {} in {}", accountId, currency);
        // Stub implementation - returns a large balance
        // In production, this would call the actual account service
        return BigDecimal.valueOf(100000);
    }
}

