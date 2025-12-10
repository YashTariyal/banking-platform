package com.banking.kyc.domain;

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
@Table(name = "screening_results")
public class ScreeningResult {

    @Id
    private UUID id;

    @Column(name = "kyc_case_id", nullable = false)
    private UUID kycCaseId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "screening_type", nullable = false, length = 32)
    private ScreeningType screeningType;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 32)
    private ScreeningResultStatus result;

    @Column(name = "match_score")
    private Integer matchScore; // 0-100

    @Column(name = "matched_name", length = 255)
    private String matchedName;

    @Column(name = "matched_list", length = 100)
    private String matchedList; // SANCTIONS, PEP, ADVERSE_MEDIA, etc.

    @Column(name = "match_details", columnDefinition = "TEXT")
    private String matchDetails;

    @Column(name = "screening_provider", length = 100)
    private String screeningProvider;

    @Column(name = "screening_reference", length = 255)
    private String screeningReference;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

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

    public UUID getKycCaseId() {
        return kycCaseId;
    }

    public void setKycCaseId(UUID kycCaseId) {
        this.kycCaseId = kycCaseId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public ScreeningType getScreeningType() {
        return screeningType;
    }

    public void setScreeningType(ScreeningType screeningType) {
        this.screeningType = screeningType;
    }

    public ScreeningResultStatus getResult() {
        return result;
    }

    public void setResult(ScreeningResultStatus result) {
        this.result = result;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public String getMatchedName() {
        return matchedName;
    }

    public void setMatchedName(String matchedName) {
        this.matchedName = matchedName;
    }

    public String getMatchedList() {
        return matchedList;
    }

    public void setMatchedList(String matchedList) {
        this.matchedList = matchedList;
    }

    public String getMatchDetails() {
        return matchDetails;
    }

    public void setMatchDetails(String matchDetails) {
        this.matchDetails = matchDetails;
    }

    public String getScreeningProvider() {
        return screeningProvider;
    }

    public void setScreeningProvider(String screeningProvider) {
        this.screeningProvider = screeningProvider;
    }

    public String getScreeningReference() {
        return screeningReference;
    }

    public void setScreeningReference(String screeningReference) {
        this.screeningReference = screeningReference;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UUID reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
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

