package com.banking.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "account_goals")
public class AccountGoal {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "target_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentAmount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AccountGoalStatus status;

    @Column(name = "auto_sweep_enabled", nullable = false)
    private boolean autoSweepEnabled;

    @Column(name = "auto_sweep_amount", precision = 19, scale = 4)
    private BigDecimal autoSweepAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "auto_sweep_cadence", length = 32)
    private AccountGoalCadence autoSweepCadence;

    @Column(name = "last_sweep_at")
    private Instant lastSweepAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
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

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = currentAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public AccountGoalStatus getStatus() {
        return status;
    }

    public void setStatus(AccountGoalStatus status) {
        this.status = status;
    }

    public boolean isAutoSweepEnabled() {
        return autoSweepEnabled;
    }

    public void setAutoSweepEnabled(boolean autoSweepEnabled) {
        this.autoSweepEnabled = autoSweepEnabled;
    }

    public BigDecimal getAutoSweepAmount() {
        return autoSweepAmount;
    }

    public void setAutoSweepAmount(BigDecimal autoSweepAmount) {
        this.autoSweepAmount = autoSweepAmount;
    }

    public AccountGoalCadence getAutoSweepCadence() {
        return autoSweepCadence;
    }

    public void setAutoSweepCadence(AccountGoalCadence autoSweepCadence) {
        this.autoSweepCadence = autoSweepCadence;
    }

    public Instant getLastSweepAt() {
        return lastSweepAt;
    }

    public void setLastSweepAt(Instant lastSweepAt) {
        this.lastSweepAt = lastSweepAt;
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

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}

