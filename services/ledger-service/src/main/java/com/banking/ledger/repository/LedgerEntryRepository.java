package com.banking.ledger.repository;

import com.banking.ledger.domain.LedgerEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Page<LedgerEntry> findByLedgerAccountId(UUID ledgerAccountId, Pageable pageable);

    List<LedgerEntry> findByJournalId(UUID journalId);
}

