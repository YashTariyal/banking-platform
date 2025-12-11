package com.banking.ledger.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public class PostJournalRequest {

    @NotBlank
    @Size(max = 64)
    private String referenceId;

    @Size(max = 255)
    private String description;

    @NotEmpty
    @Valid
    private List<JournalEntryRequest> entries;

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

    public List<JournalEntryRequest> getEntries() {
        return entries;
    }

    public void setEntries(List<JournalEntryRequest> entries) {
        this.entries = entries;
    }
}

