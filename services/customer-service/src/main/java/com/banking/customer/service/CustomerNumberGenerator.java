package com.banking.customer.service;

import com.banking.customer.repository.CustomerRepository;
import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class CustomerNumberGenerator {

    private static final String PREFIX = "CUST";
    private static final int NUMBER_LENGTH = 12;
    private static final Random random = new Random();

    private final CustomerRepository customerRepository;

    public CustomerNumberGenerator(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public String generate() {
        String customerNumber;
        int attempts = 0;
        int maxAttempts = 100;

        do {
            customerNumber = generateRandomNumber();
            attempts++;
            if (attempts >= maxAttempts) {
                throw new IllegalStateException("Unable to generate unique customer number after " + maxAttempts + " attempts");
            }
        } while (customerRepository.existsByCustomerNumber(customerNumber));

        return customerNumber;
    }

    private String generateRandomNumber() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < NUMBER_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}

