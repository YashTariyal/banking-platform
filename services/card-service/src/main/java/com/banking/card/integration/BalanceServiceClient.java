package com.banking.card.integration;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Interface for balance verification service integration.
 * In a real implementation, this would be a Feign client or REST template
 * that calls an external account service.
 */
public interface BalanceServiceClient {
    
    /**
     * Verifies if the account has sufficient balance for a transaction.
     * 
     * @param accountId The account ID to check
     * @param amount The transaction amount
     * @param currency The currency code
     * @return true if sufficient balance exists, false otherwise
     */
    boolean hasSufficientBalance(UUID accountId, BigDecimal amount, String currency);
    
    /**
     * Gets the available balance for an account.
     * 
     * @param accountId The account ID
     * @param currency The currency code
     * @return The available balance
     */
    BigDecimal getAvailableBalance(UUID accountId, String currency);
}

