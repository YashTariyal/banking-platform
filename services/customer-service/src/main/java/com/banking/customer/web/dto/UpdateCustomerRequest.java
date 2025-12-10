package com.banking.customer.web.dto;

import com.banking.customer.domain.CustomerStatus;
import com.banking.customer.domain.Gender;
import java.time.LocalDate;

public record UpdateCustomerRequest(
        String firstName,
        String lastName,
        String middleName,
        LocalDate dateOfBirth,
        Gender gender,
        CustomerStatus status
) {
}

