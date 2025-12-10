package com.banking.customer.messaging;

import com.banking.customer.domain.Customer;
import com.banking.customer.domain.CustomerStatus;
import com.banking.customer.domain.CustomerType;
import java.time.Instant;
import java.util.UUID;

public record CustomerEvent(
        UUID customerId,
        String customerNumber,
        String firstName,
        String lastName,
        CustomerType customerType,
        CustomerStatus status,
        String kycStatus,
        String eventType,
        Instant occurredAt
) {

    public static CustomerEvent created(Customer customer) {
        return fromCustomer(customer, "CUSTOMER_CREATED");
    }

    public static CustomerEvent updated(Customer customer) {
        return fromCustomer(customer, "CUSTOMER_UPDATED");
    }

    public static CustomerEvent deleted(Customer customer) {
        return fromCustomer(customer, "CUSTOMER_DELETED");
    }

    private static CustomerEvent fromCustomer(Customer customer, String eventType) {
        return new CustomerEvent(
                customer.getId(),
                customer.getCustomerNumber(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getCustomerType(),
                customer.getStatus(),
                customer.getKycStatus(),
                eventType,
                Instant.now()
        );
    }
}

