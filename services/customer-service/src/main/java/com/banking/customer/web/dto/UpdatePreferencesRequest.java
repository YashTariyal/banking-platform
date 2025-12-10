package com.banking.customer.web.dto;

public record UpdatePreferencesRequest(
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
        String preferredContactMethod
) {
}

