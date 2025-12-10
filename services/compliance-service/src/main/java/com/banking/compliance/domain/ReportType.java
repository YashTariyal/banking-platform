package com.banking.compliance.domain;

public enum ReportType {
    CTR, // Currency Transaction Report
    SAR, // Suspicious Activity Report
    LCTR, // Large Cash Transaction Report
    AML_MONTHLY,
    AML_QUARTERLY,
    AML_ANNUAL,
    SANCTIONS_SCREENING,
    PEP_SCREENING
}

