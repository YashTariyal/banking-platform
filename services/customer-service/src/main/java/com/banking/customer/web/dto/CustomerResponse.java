package com.banking.customer.web.dto;

import com.banking.customer.domain.CustomerStatus;
import com.banking.customer.domain.CustomerType;
import com.banking.customer.domain.Gender;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String customerNumber,
        CustomerStatus status,
        String firstName,
        String lastName,
        String middleName,
        LocalDate dateOfBirth,
        Gender gender,
        String nationalId,
        String nationalIdType,
        CustomerType customerType,
        String kycStatus,
        Instant kycVerifiedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

