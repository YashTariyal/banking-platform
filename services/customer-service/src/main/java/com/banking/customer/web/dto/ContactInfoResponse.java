package com.banking.customer.web.dto;

import com.banking.customer.domain.ContactType;
import java.time.Instant;
import java.util.UUID;

public record ContactInfoResponse(
        UUID id,
        UUID customerId,
        ContactType contactType,
        String email,
        String phone,
        String mobile,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        Boolean isPrimary,
        Boolean isVerified,
        Instant verifiedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

