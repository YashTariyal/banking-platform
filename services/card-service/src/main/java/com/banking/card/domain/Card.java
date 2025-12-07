package com.banking.card.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cards")
public class Card {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "card_number", nullable = false, unique = true, length = 32)
    private String cardNumber;

    @Column(name = "masked_number", nullable = false, length = 32)
    private String maskedNumber;

    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private CardStatus status;

    @Column(name = "type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private CardType type;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "spending_limit", nullable = false)
    private BigDecimal spendingLimit;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    // Transaction limits
    @Column(name = "daily_transaction_limit")
    private BigDecimal dailyTransactionLimit;

    @Column(name = "monthly_transaction_limit")
    private BigDecimal monthlyTransactionLimit;

    // PIN management
    @Column(name = "pin_hash", length = 255)
    private String pinHash;

    @Column(name = "pin_attempts")
    private Integer pinAttempts;

    @Column(name = "pin_locked_until")
    private Instant pinLockedUntil;

    // Freeze/unfreeze
    @Column(name = "frozen")
    private Boolean frozen;

    @Column(name = "frozen_at")
    private Instant frozenAt;

    @Column(name = "frozen_reason", length = 255)
    private String frozenReason;

    // Expiration date
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "issued_at")
    private Instant issuedAt;

    // Card replacement
    @Column(name = "replaced_by_card_id")
    private UUID replacedByCardId;

    @Column(name = "replacement_reason", length = 255)
    private String replacementReason;

    @Column(name = "is_replacement")
    private Boolean isReplacement;

    // Account linking
    @Column(name = "account_id")
    private UUID accountId;

    // CVV management
    @Column(name = "cvv_hash", length = 255)
    private String cvvHash;

    @Column(name = "cvv_generated_at")
    private Instant cvvGeneratedAt;

    @Column(name = "cvv_rotation_due_date")
    private Instant cvvRotationDueDate;

    // Cardholder name
    @Column(name = "cardholder_name", length = 255)
    private String cardholderName;

    // ATM withdrawal limits
    @Column(name = "daily_atm_limit")
    private BigDecimal dailyAtmLimit;

    @Column(name = "monthly_atm_limit")
    private BigDecimal monthlyAtmLimit;

    // Card renewal tracking
    @Column(name = "renewed_from_card_id")
    private UUID renewedFromCardId;

    @Column(name = "renewal_count")
    private Integer renewalCount;

    @Column(name = "last_renewed_at")
    private Instant lastRenewedAt;

    // Contactless payment controls
    @Column(name = "contactless_enabled")
    private Boolean contactlessEnabled;

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

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getMaskedNumber() {
        return maskedNumber;
    }

    public void setMaskedNumber(String maskedNumber) {
        this.maskedNumber = maskedNumber;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public CardType getType() {
        return type;
    }

    public void setType(CardType type) {
        this.type = type;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getSpendingLimit() {
        return spendingLimit;
    }

    public void setSpendingLimit(BigDecimal spendingLimit) {
        this.spendingLimit = spendingLimit;
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

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public BigDecimal getDailyTransactionLimit() {
        return dailyTransactionLimit;
    }

    public void setDailyTransactionLimit(BigDecimal dailyTransactionLimit) {
        this.dailyTransactionLimit = dailyTransactionLimit;
    }

    public BigDecimal getMonthlyTransactionLimit() {
        return monthlyTransactionLimit;
    }

    public void setMonthlyTransactionLimit(BigDecimal monthlyTransactionLimit) {
        this.monthlyTransactionLimit = monthlyTransactionLimit;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public Integer getPinAttempts() {
        return pinAttempts;
    }

    public void setPinAttempts(Integer pinAttempts) {
        this.pinAttempts = pinAttempts;
    }

    public Instant getPinLockedUntil() {
        return pinLockedUntil;
    }

    public void setPinLockedUntil(Instant pinLockedUntil) {
        this.pinLockedUntil = pinLockedUntil;
    }

    public Boolean getFrozen() {
        return frozen;
    }

    public void setFrozen(Boolean frozen) {
        this.frozen = frozen;
    }

    public Instant getFrozenAt() {
        return frozenAt;
    }

    public void setFrozenAt(Instant frozenAt) {
        this.frozenAt = frozenAt;
    }

    public String getFrozenReason() {
        return frozenReason;
    }

    public void setFrozenReason(String frozenReason) {
        this.frozenReason = frozenReason;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public UUID getReplacedByCardId() {
        return replacedByCardId;
    }

    public void setReplacedByCardId(UUID replacedByCardId) {
        this.replacedByCardId = replacedByCardId;
    }

    public String getReplacementReason() {
        return replacementReason;
    }

    public void setReplacementReason(String replacementReason) {
        this.replacementReason = replacementReason;
    }

    public Boolean getIsReplacement() {
        return isReplacement;
    }

    public void setIsReplacement(Boolean isReplacement) {
        this.isReplacement = isReplacement;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getCvvHash() {
        return cvvHash;
    }

    public void setCvvHash(String cvvHash) {
        this.cvvHash = cvvHash;
    }

    public Instant getCvvGeneratedAt() {
        return cvvGeneratedAt;
    }

    public void setCvvGeneratedAt(Instant cvvGeneratedAt) {
        this.cvvGeneratedAt = cvvGeneratedAt;
    }

    public Instant getCvvRotationDueDate() {
        return cvvRotationDueDate;
    }

    public void setCvvRotationDueDate(Instant cvvRotationDueDate) {
        this.cvvRotationDueDate = cvvRotationDueDate;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    public BigDecimal getDailyAtmLimit() {
        return dailyAtmLimit;
    }

    public void setDailyAtmLimit(BigDecimal dailyAtmLimit) {
        this.dailyAtmLimit = dailyAtmLimit;
    }

    public BigDecimal getMonthlyAtmLimit() {
        return monthlyAtmLimit;
    }

    public void setMonthlyAtmLimit(BigDecimal monthlyAtmLimit) {
        this.monthlyAtmLimit = monthlyAtmLimit;
    }

    public UUID getRenewedFromCardId() {
        return renewedFromCardId;
    }

    public void setRenewedFromCardId(UUID renewedFromCardId) {
        this.renewedFromCardId = renewedFromCardId;
    }

    public Integer getRenewalCount() {
        return renewalCount;
    }

    public void setRenewalCount(Integer renewalCount) {
        this.renewalCount = renewalCount;
    }

    public Instant getLastRenewedAt() {
        return lastRenewedAt;
    }

    public void setLastRenewedAt(Instant lastRenewedAt) {
        this.lastRenewedAt = lastRenewedAt;
    }

    public Boolean getContactlessEnabled() {
        return contactlessEnabled;
    }

    public void setContactlessEnabled(Boolean contactlessEnabled) {
        this.contactlessEnabled = contactlessEnabled;
    }
}


