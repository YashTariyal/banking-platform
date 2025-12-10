package com.banking.compliance.domain;

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
import java.util.UUID;

@Entity
@Table(name = "suspicious_activities")
public class SuspiciousActivity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 64)
    private SuspiciousActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 32)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SuspiciousActivityStatus status;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(name = "compliance_record_id")
    private UUID complianceRecordId;

    @Column(name = "investigator_id")
    private UUID investigatorId;

    @Column(name = "investigation_notes", columnDefinition = "TEXT")
    private String investigationNotes;

    @Column(name = "reported_at")
    private Instant reportedAt;

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
        if (status == null) {
            status = SuspiciousActivityStatus.OPEN;
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

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public SuspiciousActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(SuspiciousActivityType activityType) {
        this.activityType = activityType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public SuspiciousActivityStatus getStatus() {
        return status;
    }

    public void setStatus(SuspiciousActivityStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public UUID getComplianceRecordId() {
        return complianceRecordId;
    }

    public void setComplianceRecordId(UUID complianceRecordId) {
        this.complianceRecordId = complianceRecordId;
    }

    public UUID getInvestigatorId() {
        return investigatorId;
    }

    public void setInvestigatorId(UUID investigatorId) {
        this.investigatorId = investigatorId;
    }

    public String getInvestigationNotes() {
        return investigationNotes;
    }

    public void setInvestigationNotes(String investigationNotes) {
        this.investigationNotes = investigationNotes;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(Instant reportedAt) {
        this.reportedAt = reportedAt;
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

