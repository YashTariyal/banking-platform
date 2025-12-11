package com.banking.ledger.service;

import com.banking.ledger.domain.LedgerAccount;
import com.banking.ledger.domain.LedgerAccountStatus;
import com.banking.ledger.repository.LedgerAccountRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LedgerAccountService {

    private final LedgerAccountRepository ledgerAccountRepository;

    public LedgerAccountService(LedgerAccountRepository ledgerAccountRepository) {
        this.ledgerAccountRepository = ledgerAccountRepository;
    }

    @Transactional
    public LedgerAccount create(LedgerAccount account) {
        if (account.getExternalAccountId() != null
                && ledgerAccountRepository.existsByExternalAccountId(account.getExternalAccountId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "External account id already exists");
        }
        return ledgerAccountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public LedgerAccount get(UUID id) {
        return ledgerAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ledger account not found"));
    }

    @Transactional(readOnly = true)
    public Page<LedgerAccount> list(Pageable pageable) {
        return ledgerAccountRepository.findAll(pageable);
    }

    @Transactional
    public LedgerAccount updateStatus(UUID id, LedgerAccountStatus status) {
        LedgerAccount account = get(id);
        account.setStatus(status);
        return ledgerAccountRepository.save(account);
    }
}

