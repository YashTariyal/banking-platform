package com.banking.ledger.web.dto;

import com.banking.ledger.domain.JournalStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class LedgerJournalResponse {

    private UUID id;
    private String referenceId;
    private String description;
    private JournalStatus status;
    private Instant postedAt;
    private UUID reversalOf;
    private List<LedgerEntryResponse> entries;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JournalStatus getStatus() {
        return status;
    }

    public void setStatus(JournalStatus status) {
        this.status = status;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Instant postedAt) {
        this.postedAt = postedAt;
    }

    public UUID getReversalOf() {
        return reversalOf;
    }

    public void setReversalOf(UUID reversalOf) {
        this.reversalOf = reversalOf;
    }

    public List<LedgerEntryResponse> getEntries() {
        return entries;
    }

    public void setEntries(List<LedgerEntryResponse> entries) {
        this.entries = entries;
    }
}

