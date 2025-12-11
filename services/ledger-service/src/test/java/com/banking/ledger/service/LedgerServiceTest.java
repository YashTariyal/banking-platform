package com.banking.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerAccountRepository accountRepository;
    @Mock
    private LedgerJournalRepository journalRepository;
    @Mock
    private LedgerEntryRepository entryRepository;

    private Clock clock;

    private LedgerService ledgerService;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        ledgerService = new LedgerService(accountRepository, journalRepository, entryRepository, clock);
    }

    @Test
    void postJournal_balancedEntries_updatesBalancesAndSaves() {
        LedgerAccount cash = buildAccount(LedgerAccountType.ASSET);
        LedgerAccount payable = buildAccount(LedgerAccountType.LIABILITY);

        LedgerJournal journal = new LedgerJournal();
        journal.setReferenceId("REF-1");

        LedgerEntry debit = buildEntry(cash.getId(), EntryType.DEBIT, new BigDecimal("100.00"));
        LedgerEntry credit = buildEntry(payable.getId(), EntryType.CREDIT, new BigDecimal("100.00"));

        when(journalRepository.findByReferenceId(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findAllById(anyList())).thenReturn(List.of(cash, payable));
        when(journalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(entryRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerJournal saved = ledgerService.postJournal(journal, List.of(debit, credit));

        assertThat(saved.getStatus()).isEqualTo(JournalStatus.POSTED);
        assertThat(saved.getPostedAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));

        assertThat(cash.getBalance()).isEqualTo(new BigDecimal("100.00"));
        assertThat(payable.getBalance()).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void postJournal_unbalanced_throwsBadRequest() {
        LedgerAccount cash = buildAccount(LedgerAccountType.ASSET);
        LedgerEntry debit = buildEntry(cash.getId(), EntryType.DEBIT, new BigDecimal("50.00"));
        LedgerEntry credit = buildEntry(cash.getId(), EntryType.CREDIT, new BigDecimal("10.00"));

        when(journalRepository.findByReferenceId(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findAllById(anyList())).thenReturn(List.of(cash));

        LedgerJournal journal = new LedgerJournal();
        journal.setReferenceId("UNBALANCED");

        assertThatThrownBy(() -> ledgerService.postJournal(journal, List.of(debit, credit)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void reverseJournal_createsReversalAndMarksOriginal() {
        UUID journalId = UUID.randomUUID();
        LedgerJournal original = new LedgerJournal();
        original.setId(journalId);
        original.setReferenceId("REF-2");
        original.setStatus(JournalStatus.POSTED);

        LedgerAccount cash = buildAccount(LedgerAccountType.ASSET);
        LedgerAccount revenue = buildAccount(LedgerAccountType.INCOME);

        LedgerEntry debit = buildEntry(cash.getId(), EntryType.DEBIT, new BigDecimal("200.00"));
        LedgerEntry credit = buildEntry(revenue.getId(), EntryType.CREDIT, new BigDecimal("200.00"));

        when(journalRepository.findById(journalId)).thenReturn(Optional.of(original));
        when(entryRepository.findByJournalId(journalId)).thenReturn(List.of(debit, credit));
        when(journalRepository.findByReferenceId(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findAllById(anyList())).thenReturn(List.of(cash, revenue));
        when(journalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(entryRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerJournal reversal = ledgerService.reverseJournal(journalId, "cleanup");

        assertThat(reversal.getReferenceId()).isEqualTo("REF-2-REV");
        assertThat(reversal.getStatus()).isEqualTo(JournalStatus.POSTED);
        verify(journalRepository).save(original);
        assertThat(original.getStatus()).isEqualTo(JournalStatus.REVERSED);
    }

    private LedgerAccount buildAccount(LedgerAccountType type) {
        LedgerAccount account = new LedgerAccount();
        account.setId(UUID.randomUUID());
        account.setName("acct-" + type);
        account.setType(type);
        account.setCurrency("USD");
        account.setStatus(LedgerAccountStatus.ACTIVE);
        account.setBalance(BigDecimal.ZERO);
        return account;
    }

    private LedgerEntry buildEntry(UUID accountId, EntryType type, BigDecimal amount) {
        LedgerEntry entry = new LedgerEntry();
        entry.setLedgerAccountId(accountId);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setCurrency("USD");
        return entry;
    }
}

