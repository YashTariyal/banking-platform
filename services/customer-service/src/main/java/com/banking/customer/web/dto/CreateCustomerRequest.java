package com.banking.customer.web.dto;

import com.banking.customer.domain.CustomerType;
import com.banking.customer.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateCustomerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String middleName,
        @NotNull LocalDate dateOfBirth,
        Gender gender,
        String nationalId,
        String nationalIdType,
        CustomerType customerType,
        String email,
        String phone,
        String mobile,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        String language,
        String currency,
        String timezone
) {
}

