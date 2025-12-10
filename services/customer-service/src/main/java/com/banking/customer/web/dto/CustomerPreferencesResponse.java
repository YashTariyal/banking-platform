package com.banking.customer.web.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerPreferencesResponse(
        UUID id,
        UUID customerId,
        String language,
        String timezone,
        String currency,
        Boolean emailNotificationsEnabled,
        Boolean smsNotificationsEnabled,
        Boolean pushNotificationsEnabled,
        Boolean marketingEmailsEnabled,
        Boolean paperStatementsEnabled,
        Boolean twoFactorEnabled,
        Boolean biometricEnabled,
        String preferredContactMethod,
        Instant createdAt,
        Instant updatedAt
) {
}

