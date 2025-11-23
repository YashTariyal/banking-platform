package com.banking.account.service;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for validating currency codes against ISO 4217 standard.
 */
@Service
public class CurrencyValidationService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyValidationService.class);

    // Common ISO 4217 currency codes
    // In production, this could be loaded from a database or external service
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "INR", "SGD",
            "HKD", "NZD", "SEK", "NOK", "DKK", "PLN", "ZAR", "BRL", "MXN", "KRW",
            "TRY", "RUB", "AED", "SAR", "THB", "MYR", "IDR", "PHP", "VND", "ILS"
    );

    /**
     * Validates that a currency code is supported.
     *
     * @param currency The currency code to validate (must be uppercase ISO 4217 code)
     * @throws UnsupportedCurrencyException if currency is not supported
     */
    public void validateCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new UnsupportedCurrencyException("Currency code cannot be null or empty");
        }

        String upperCurrency = currency.toUpperCase();
        
        if (!upperCurrency.matches("^[A-Z]{3}$")) {
            throw new UnsupportedCurrencyException(
                    String.format("Invalid currency code format: %s. Must be 3 uppercase letters (ISO 4217)", currency));
        }

        if (!SUPPORTED_CURRENCIES.contains(upperCurrency)) {
            log.warn("Unsupported currency code: {}. Supported currencies: {}", currency, SUPPORTED_CURRENCIES);
            throw new UnsupportedCurrencyException(
                    String.format("Currency %s is not supported. Supported currencies: %s", 
                            currency, SUPPORTED_CURRENCIES));
        }

        log.debug("Currency validation passed: {}", upperCurrency);
    }

    /**
     * Gets the list of supported currencies.
     */
    public Set<String> getSupportedCurrencies() {
        return Set.copyOf(SUPPORTED_CURRENCIES);
    }
}

