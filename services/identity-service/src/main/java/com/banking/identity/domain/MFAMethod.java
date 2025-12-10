package com.banking.identity.domain;

public enum MFAMethod {
    TOTP,      // Time-based One-Time Password (Google Authenticator, Authy)
    SMS,       // SMS-based OTP
    EMAIL      // Email-based OTP
}

