package com.banking.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mfa_settings")
public class MFASettings {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "mfa_enabled", nullable = false)
    private Boolean mfaEnabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_method", length = 32)
    private MFAMethod mfaMethod;

    @Column(name = "totp_secret", length = 255)
    private String totpSecret;

    @Column(name = "totp_backup_codes", length = 1000)
    private String totpBackupCodes; // JSON array of backup codes

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "phone_verified", nullable = false)
    private Boolean phoneVerified;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

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
        if (mfaEnabled == null) {
            mfaEnabled = false;
        }
        if (phoneVerified == null) {
            phoneVerified = false;
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Boolean getMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(Boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public MFAMethod getMfaMethod() {
        return mfaMethod;
    }

    public void setMfaMethod(MFAMethod mfaMethod) {
        this.mfaMethod = mfaMethod;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public String getTotpBackupCodes() {
        return totpBackupCodes;
    }

    public void setTotpBackupCodes(String totpBackupCodes) {
        this.totpBackupCodes = totpBackupCodes;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getPhoneVerified() {
        return phoneVerified;
    }

    public void setPhoneVerified(Boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public Instant getPhoneVerifiedAt() {
        return phoneVerifiedAt;
    }

    public void setPhoneVerifiedAt(Instant phoneVerifiedAt) {
        this.phoneVerifiedAt = phoneVerifiedAt;
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

