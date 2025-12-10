package com.banking.customer.web.dto;

import com.banking.customer.domain.ContactType;
import jakarta.validation.constraints.NotNull;

public record CreateContactInfoRequest(
        @NotNull ContactType contactType,
        String email,
        String phone,
        String mobile,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        Boolean isPrimary
) {
}

