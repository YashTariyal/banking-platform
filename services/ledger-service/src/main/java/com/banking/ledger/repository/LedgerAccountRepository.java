package com.banking.ledger.repository;

import com.banking.ledger.domain.LedgerAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {

    Optional<LedgerAccount> findByExternalAccountId(String externalAccountId);

    boolean existsByExternalAccountId(String externalAccountId);
}

