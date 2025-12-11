package com.banking.ledger.repository;

import com.banking.ledger.domain.LedgerJournal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerJournalRepository extends JpaRepository<LedgerJournal, UUID> {

    Optional<LedgerJournal> findByReferenceId(String referenceId);
}

