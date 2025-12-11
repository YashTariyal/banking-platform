package com.banking.ledger.service;

import com.banking.ledger.domain.EntryType;
import com.banking.ledger.domain.JournalStatus;
import com.banking.ledger.domain.LedgerAccount;
import com.banking.ledger.domain.LedgerAccountStatus;
import com.banking.ledger.domain.LedgerAccountType;
import com.banking.ledger.domain.LedgerEntry;
import com.banking.ledger.domain.LedgerJournal;
import com.banking.ledger.repository.LedgerAccountRepository;
import com.banking.ledger.repository.LedgerEntryRepository;
import com.banking.ledger.repository.LedgerJournalRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LedgerService {

    private final LedgerAccountRepository accountRepository;
    private final LedgerJournalRepository journalRepository;
    private final LedgerEntryRepository entryRepository;
    private final Clock clock;

    public LedgerService(
            LedgerAccountRepository accountRepository,
            LedgerJournalRepository journalRepository,
            LedgerEntryRepository entryRepository,
            Clock clock
    ) {
        this.accountRepository = accountRepository;
        this.journalRepository = journalRepository;
        this.entryRepository = entryRepository;
        this.clock = clock;
    }

    @Transactional
    public LedgerJournal postJournal(LedgerJournal journal, List<LedgerEntry> entries) {
        validateNewJournal(journal, entries);

        Map<UUID, LedgerAccount> accounts = loadAccounts(entries);
        validateEntries(entries, accounts);

        // Adjust balances first to ensure optimistic locking consistency
        adjustAccountBalances(entries, accounts);
        accountRepository.saveAll(accounts.values());

        journal.setStatus(JournalStatus.POSTED);
        journal.setPostedAt(Instant.now(clock));
        LedgerJournal savedJournal = journalRepository.save(journal);

        for (LedgerEntry entry : entries) {
            entry.setJournalId(savedJournal.getId());
            entry.setPostedAt(savedJournal.getPostedAt());
        }
        entryRepository.saveAll(entries);
        return savedJournal;
    }

    @Transactional
    public LedgerJournal reverseJournal(UUID journalId, String reason) {
        LedgerJournal original = journalRepository.findById(journalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journal not found"));
        if (original.getStatus() == JournalStatus.REVERSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Journal already reversed");
        }

        List<LedgerEntry> originalEntries = entryRepository.findByJournalId(journalId);
        if (originalEntries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Journal has no entries to reverse");
        }

        LedgerJournal reversal = new LedgerJournal();
        reversal.setReferenceId(original.getReferenceId() + "-REV");
        reversal.setDescription(reason != null ? reason : "Reversal of journal " + original.getReferenceId());
        reversal.setReversalOf(original.getId());
        reversal.setStatus(JournalStatus.POSTED);
        reversal.setPostedAt(Instant.now(clock));

        List<LedgerEntry> reversalEntries = new ArrayList<>();
        for (LedgerEntry entry : originalEntries) {
            LedgerEntry reversalEntry = new LedgerEntry();
            reversalEntry.setLedgerAccountId(entry.getLedgerAccountId());
            reversalEntry.setEntryType(entry.getEntryType() == EntryType.DEBIT ? EntryType.CREDIT : EntryType.DEBIT);
            reversalEntry.setAmount(entry.getAmount());
            reversalEntry.setCurrency(entry.getCurrency());
            reversalEntry.setDescription("Reversal of " + entry.getId());
            reversalEntries.add(reversalEntry);
        }

        postJournal(reversal, reversalEntries);
        original.setStatus(JournalStatus.REVERSED);
        journalRepository.save(original);
        return reversal;
    }

    @Transactional(readOnly = true)
    public LedgerJournal getJournal(UUID id) {
        return journalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journal not found"));
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntry> getEntriesForAccount(UUID accountId, Pageable pageable) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ledger account not found");
        }
        return entryRepository.findByLedgerAccountId(accountId, pageable);
    }

    private void validateNewJournal(LedgerJournal journal, List<LedgerEntry> entries) {
        if (journal.getReferenceId() == null || journal.getReferenceId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reference id is required");
        }
        if (journalRepository.findByReferenceId(journal.getReferenceId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reference id already exists");
        }
        if (entries == null || entries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one entry is required");
        }
    }

    private Map<UUID, LedgerAccount> loadAccounts(List<LedgerEntry> entries) {
        List<UUID> accountIds = entries.stream()
                .map(LedgerEntry::getLedgerAccountId)
                .collect(Collectors.toList());
        Map<UUID, LedgerAccount> accounts = accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(LedgerAccount::getId, a -> a));
        if (accounts.size() != accountIds.stream().distinct().count()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more ledger accounts not found");
        }
        accounts.values().forEach(account -> {
            if (account.getStatus() == LedgerAccountStatus.CLOSED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account " + account.getId() + " is closed");
            }
        });
        return accounts;
    }

    private void validateEntries(List<LedgerEntry> entries, Map<UUID, LedgerAccount> accounts) {
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        for (LedgerEntry entry : entries) {
            if (entry.getAmount() == null || entry.getAmount().signum() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entry amount must be positive");
            }
            LedgerAccount account = accounts.get(entry.getLedgerAccountId());
            if (account == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown ledger account");
            }
            if (!account.getCurrency().equals(entry.getCurrency())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency mismatch for account " + account.getId());
            }
            if (entry.getEntryType() == EntryType.DEBIT) {
                totalDebits = totalDebits.add(entry.getAmount());
            } else {
                totalCredits = totalCredits.add(entry.getAmount());
            }
        }
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debits and credits must balance");
        }
        if (totalDebits.compareTo(BigDecimal.ZERO) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Journal total must be greater than zero");
        }
    }

    private void adjustAccountBalances(List<LedgerEntry> entries, Map<UUID, LedgerAccount> accounts) {
        for (LedgerEntry entry : entries) {
            LedgerAccount account = accounts.get(entry.getLedgerAccountId());
            BigDecimal delta = computeDelta(account.getType(), entry.getEntryType(), entry.getAmount());
            account.setBalance(account.getBalance().add(delta));
        }
    }

    private BigDecimal computeDelta(LedgerAccountType type, EntryType entryType, BigDecimal amount) {
        boolean debitIncreases = type == LedgerAccountType.ASSET || type == LedgerAccountType.EXPENSE;
        if (entryType == EntryType.DEBIT) {
            return debitIncreases ? amount : amount.negate();
        } else {
            return debitIncreases ? amount.negate() : amount;
        }
    }
}

