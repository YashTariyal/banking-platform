package com.banking.ledger.web;

import com.banking.ledger.domain.LedgerAccount;
import com.banking.ledger.domain.LedgerEntry;
import com.banking.ledger.domain.LedgerJournal;
import com.banking.ledger.web.dto.LedgerAccountResponse;
import com.banking.ledger.web.dto.LedgerEntryResponse;
import com.banking.ledger.web.dto.LedgerJournalResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class LedgerMapper {

    public LedgerAccountResponse toResponse(LedgerAccount account) {
        LedgerAccountResponse response = new LedgerAccountResponse();
        response.setId(account.getId());
        response.setName(account.getName());
        response.setType(account.getType());
        response.setStatus(account.getStatus());
        response.setCurrency(account.getCurrency());
        response.setExternalAccountId(account.getExternalAccountId());
        response.setBalance(account.getBalance());
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());
        return response;
    }

    public LedgerEntryResponse toResponse(LedgerEntry entry) {
        LedgerEntryResponse response = new LedgerEntryResponse();
        response.setId(entry.getId());
        response.setJournalId(entry.getJournalId());
        response.setLedgerAccountId(entry.getLedgerAccountId());
        response.setEntryType(entry.getEntryType());
        response.setAmount(entry.getAmount());
        response.setCurrency(entry.getCurrency());
        response.setDescription(entry.getDescription());
        response.setPostedAt(entry.getPostedAt());
        return response;
    }

    public LedgerJournalResponse toResponse(LedgerJournal journal, List<LedgerEntry> entries) {
        LedgerJournalResponse response = new LedgerJournalResponse();
        response.setId(journal.getId());
        response.setReferenceId(journal.getReferenceId());
        response.setDescription(journal.getDescription());
        response.setStatus(journal.getStatus());
        response.setPostedAt(journal.getPostedAt());
        response.setReversalOf(journal.getReversalOf());
        response.setEntries(entries.stream().map(this::toResponse).collect(Collectors.toList()));
        return response;
    }

    public <T, R> com.banking.ledger.web.dto.PageResponse<R> toPageResponse(Page<T> page, java.util.function.Function<T, R> mapper) {
        return new com.banking.ledger.web.dto.PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}

