package com.banking.account.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for validating customer existence.
 * Uses circuit breaker pattern for resilience when calling external customer service.
 */
@Service
public class CustomerValidationService {

    private static final Logger log = LoggerFactory.getLogger(CustomerValidationService.class);

    /**
     * Validates that a customer exists.
     * Uses circuit breaker and retry mechanisms for resilience.
     *
     * @param customerId The customer ID to validate
     * @throws CustomerNotFoundException if customer does not exist
     */
    @CircuitBreaker(name = "customer-service", fallbackMethod = "validateCustomerExistsFallback")
    @Retry(name = "customer-service")
    public void validateCustomerExists(UUID customerId) {
        log.debug("Validating customer existence: {}", customerId);
        
        // TODO: In production, this would make an HTTP call to customer service
        // For now, simulate a call that could fail
        // Example implementation:
        // try {
        //     boolean exists = customerServiceClient.exists(customerId);
        //     if (!exists) {
        //         throw new CustomerNotFoundException(customerId);
        //     }
        // } catch (Exception ex) {
        //     log.error("Error validating customer: {}", customerId, ex);
        //     throw ex;
        // }
        
        // Placeholder: Assume customer exists for now
        // In real implementation, this would call customer service
    }

    /**
     * Fallback method when circuit breaker is open or service is unavailable.
     * In a real scenario, this might:
     * - Check a local cache
     * - Use a default policy (allow/deny)
     * - Log for manual review
     */
    @SuppressWarnings("unused")
    public void validateCustomerExistsFallback(UUID customerId, Exception ex) {
        log.warn("Customer service unavailable, using fallback validation. customerId={} error={}", 
                customerId, ex.getMessage());
        
        // Fallback strategy: For now, we allow the operation to proceed
        // In production, you might want to:
        // 1. Check a local cache of validated customers
        // 2. Use a more restrictive policy (e.g., reject if not in cache)
        // 3. Queue for later validation
        
        log.info("Fallback: Allowing account creation for customerId={} (customer service unavailable)", customerId);
        // Could throw CustomerServiceUnavailableException if you want to be more restrictive
    }
}

