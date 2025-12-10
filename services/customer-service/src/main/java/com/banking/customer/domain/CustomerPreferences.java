package com.banking.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_preferences")
public class CustomerPreferences {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false, unique = true)
    private UUID customerId;

    @Column(name = "language", length = 10)
    private String language; // ISO 639-1 code (e.g., "en", "es")

    @Column(name = "timezone", length = 50)
    private String timezone; // e.g., "America/New_York"

    @Column(name = "currency", length = 3)
    private String currency; // ISO 4217 code (e.g., "USD")

    @Column(name = "email_notifications_enabled", nullable = false)
    private Boolean emailNotificationsEnabled;

    @Column(name = "sms_notifications_enabled", nullable = false)
    private Boolean smsNotificationsEnabled;

    @Column(name = "push_notifications_enabled", nullable = false)
    private Boolean pushNotificationsEnabled;

    @Column(name = "marketing_emails_enabled", nullable = false)
    private Boolean marketingEmailsEnabled;

    @Column(name = "paper_statements_enabled", nullable = false)
    private Boolean paperStatementsEnabled;

    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled;

    @Column(name = "biometric_enabled")
    private Boolean biometricEnabled;

    @Column(name = "preferred_contact_method", length = 32)
    private String preferredContactMethod; // EMAIL, SMS, PHONE, PUSH

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        // Set defaults
        if (emailNotificationsEnabled == null) {
            emailNotificationsEnabled = true;
        }
        if (smsNotificationsEnabled == null) {
            smsNotificationsEnabled = false;
        }
        if (pushNotificationsEnabled == null) {
            pushNotificationsEnabled = true;
        }
        if (marketingEmailsEnabled == null) {
            marketingEmailsEnabled = false;
        }
        if (paperStatementsEnabled == null) {
            paperStatementsEnabled = false;
        }
        if (twoFactorEnabled == null) {
            twoFactorEnabled = false;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Boolean getEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(Boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public Boolean getSmsNotificationsEnabled() {
        return smsNotificationsEnabled;
    }

    public void setSmsNotificationsEnabled(Boolean smsNotificationsEnabled) {
        this.smsNotificationsEnabled = smsNotificationsEnabled;
    }

    public Boolean getPushNotificationsEnabled() {
        return pushNotificationsEnabled;
    }

    public void setPushNotificationsEnabled(Boolean pushNotificationsEnabled) {
        this.pushNotificationsEnabled = pushNotificationsEnabled;
    }

    public Boolean getMarketingEmailsEnabled() {
        return marketingEmailsEnabled;
    }

    public void setMarketingEmailsEnabled(Boolean marketingEmailsEnabled) {
        this.marketingEmailsEnabled = marketingEmailsEnabled;
    }

    public Boolean getPaperStatementsEnabled() {
        return paperStatementsEnabled;
    }

    public void setPaperStatementsEnabled(Boolean paperStatementsEnabled) {
        this.paperStatementsEnabled = paperStatementsEnabled;
    }

    public Boolean getTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public Boolean getBiometricEnabled() {
        return biometricEnabled;
    }

    public void setBiometricEnabled(Boolean biometricEnabled) {
        this.biometricEnabled = biometricEnabled;
    }

    public String getPreferredContactMethod() {
        return preferredContactMethod;
    }

    public void setPreferredContactMethod(String preferredContactMethod) {
        this.preferredContactMethod = preferredContactMethod;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

