package com.banking.ledger.web;

import com.banking.ledger.domain.LedgerEntry;
import com.banking.ledger.domain.LedgerJournal;
import com.banking.ledger.repository.LedgerEntryRepository;
import com.banking.ledger.service.LedgerService;
import com.banking.ledger.web.dto.JournalEntryRequest;
import com.banking.ledger.web.dto.LedgerEntryResponse;
import com.banking.ledger.web.dto.LedgerJournalResponse;
import com.banking.ledger.web.dto.PageResponse;
import com.banking.ledger.web.dto.PostJournalRequest;
import com.banking.ledger.web.dto.ReverseJournalRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ledger")
public class LedgerJournalController {

    private final LedgerService ledgerService;
    private final LedgerEntryRepository entryRepository;
    private final LedgerMapper mapper;

    public LedgerJournalController(
            LedgerService ledgerService,
            LedgerEntryRepository entryRepository,
            LedgerMapper mapper
    ) {
        this.ledgerService = ledgerService;
        this.entryRepository = entryRepository;
        this.mapper = mapper;
    }

    @PostMapping("/journals")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerJournalResponse postJournal(@Valid @RequestBody PostJournalRequest request) {
        LedgerJournal journal = new LedgerJournal();
        journal.setReferenceId(request.getReferenceId());
        journal.setDescription(request.getDescription());

        List<LedgerEntry> entries = new ArrayList<>();
        for (JournalEntryRequest entryRequest : request.getEntries()) {
            LedgerEntry entry = new LedgerEntry();
            entry.setLedgerAccountId(entryRequest.getLedgerAccountId());
            entry.setEntryType(entryRequest.getEntryType());
            entry.setAmount(entryRequest.getAmount().setScale(4, RoundingMode.HALF_UP));
            entry.setCurrency(entryRequest.getCurrency());
            entry.setDescription(entryRequest.getDescription());
            entries.add(entry);
        }

        LedgerJournal saved = ledgerService.postJournal(journal, entries);
        List<LedgerEntry> savedEntries = entryRepository.findByJournalId(saved.getId());
        return mapper.toResponse(saved, savedEntries);
    }

    @PostMapping("/journals/{id}/reverse")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerJournalResponse reverseJournal(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ReverseJournalRequest request
    ) {
        LedgerJournal reversal = ledgerService.reverseJournal(id, request != null ? request.getReason() : null);
        List<LedgerEntry> savedEntries = entryRepository.findByJournalId(reversal.getId());
        return mapper.toResponse(reversal, savedEntries);
    }

    @GetMapping("/journals/{id}")
    public LedgerJournalResponse getJournal(@PathVariable UUID id) {
        LedgerJournal journal = ledgerService.getJournal(id);
        List<LedgerEntry> entries = entryRepository.findByJournalId(id);
        return mapper.toResponse(journal, entries);
    }

    @GetMapping("/accounts/{accountId}/entries")
    public PageResponse<LedgerEntryResponse> getEntries(
            @PathVariable UUID accountId,
            @ParameterObject Pageable pageable
    ) {
        Page<LedgerEntry> page = ledgerService.getEntriesForAccount(accountId, normalize(pageable));
        return mapper.toPageResponse(page, mapper::toResponse);
    }

    private Pageable normalize(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20);
        }
        int size = Math.min(pageable.getPageSize(), 200);
        return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
    }
}

