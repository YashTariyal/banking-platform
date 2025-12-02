package com.banking.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.account.domain.Account;
import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountTransactionLog;
import com.banking.account.domain.AccountTransactionType;
import com.banking.account.domain.AccountType;
import com.banking.account.metrics.AccountMetrics;
import com.banking.account.messaging.AccountEventPublisher;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.AccountTransactionLogRepository;
import com.banking.account.web.dto.AccountResponse;
import com.banking.account.web.dto.AccountTransactionRequest;
import com.banking.account.web.dto.BalanceResponse;
import com.banking.account.web.dto.CreateAccountRequest;
import com.banking.account.web.dto.PageResponse;
import com.banking.account.web.dto.TransactionHistoryResponse;
import com.banking.account.web.dto.UpdateAccountRequest;
import com.banking.account.web.dto.UpdateAccountStatusRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.mockito.Mockito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountTransactionLogRepository transactionLogRepository;
    @Mock
    private AccountNumberGenerator accountNumberGenerator;
    @Mock
    private AccountEventPublisher eventPublisher;
    @Mock
    private AccountMetrics accountMetrics;
    @Mock
    private CustomerValidationService customerValidationService;
    @Mock
    private CurrencyValidationService currencyValidationService;
    @Mock
    private com.banking.account.config.AccountLimitsProperties accountLimits;
    @Mock
    private com.banking.account.messaging.TransactionAuditLogger auditLogger;
    @Mock
    private com.banking.account.security.TenantAccessEvaluator tenantAccessEvaluator;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        // Setup default account limits
        when(accountLimits.getMinBalance()).thenReturn(BigDecimal.ZERO);
        when(accountLimits.getMaxBalance()).thenReturn(new BigDecimal("999999999.99"));
        when(accountLimits.getMaxTransactionAmount()).thenReturn(new BigDecimal("1000000.00"));
        when(accountLimits.getMaxDailyTransactions()).thenReturn(100);
        when(accountLimits.getMaxDailyTransactionAmount()).thenReturn(new BigDecimal("50000.00"));

        accountService = new AccountService(
                accountRepository,
                transactionLogRepository,
                accountNumberGenerator,
                eventPublisher,
                accountMetrics,
                customerValidationService,
                currencyValidationService,
                accountLimits,
                auditLogger,
                tenantAccessEvaluator
        );
    }

    @Test
    void createAccountPersistsAndEmitsEvent() {
        UUID customerId = UUID.randomUUID();
        CreateAccountRequest request = new CreateAccountRequest(customerId, AccountType.CHECKING, "USD", BigDecimal.valueOf(250));
        when(accountNumberGenerator.generate()).thenReturn("ACC-123456789012");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setOpenedAt(Instant.now());
            account.setUpdatedAt(Instant.now());
            return account;
        });

        AccountResponse response = accountService.createAccount(request);

        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.balance()).isEqualByComparingTo("250.00");
        verify(customerValidationService).validateCustomerExists(customerId);
        verify(currencyValidationService).validateCurrency("USD");
        verify(eventPublisher).publishAccountCreated(any(Account.class));
        verify(accountMetrics).incrementCreated();
        verify(accountMetrics).recordBalance(any(BigDecimal.class));
    }

    @Test
    void listAccountsReturnsPagedResponse() {
        PageImpl<Account> page = new PageImpl<>(java.util.List.of(buildAccount(UUID.randomUUID(), UUID.randomUUID())));
        when(accountRepository.findAll(eq(PageRequest.of(0, 20)))).thenReturn(page);

        PageResponse<AccountResponse> response = accountService.listAccounts(null, 0, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void updateStatusPublishesEventAndMetrics() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        accountService.updateStatus(account.getId(), new UpdateAccountStatusRequest(AccountStatus.SUSPENDED));

        assertThat(account.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        verify(eventPublisher).publishAccountUpdated(account);
        verify(accountMetrics).incrementUpdated();
    }

    @Test
    void applyTransactionDebitReducesBalance() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        account.setBalance(new BigDecimal("200.00"));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        UUID referenceId = UUID.randomUUID();
        when(transactionLogRepository.findByAccountIdAndReferenceId(account.getId(), referenceId)).thenReturn(Optional.empty());
        when(transactionLogRepository.countByAccountIdAndDateRange(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(0L);
        when(transactionLogRepository.sumAmountByAccountIdAndDateRange(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(BigDecimal.ZERO);

        AccountTransactionRequest request = new AccountTransactionRequest(referenceId, AccountTransactionType.DEBIT, new BigDecimal("50.00"), "ATM");
        AccountResponse response = accountService.applyTransaction(account.getId(), request);

        assertThat(response.balance()).isEqualByComparingTo("150.00");
        verify(accountMetrics).incrementDebit();
        verify(accountMetrics).recordTransactionAmount(any(BigDecimal.class), eq(AccountTransactionType.DEBIT));
        verify(accountMetrics).recordBalance(any(BigDecimal.class));
    }

    @Test
    void applyTransactionDebitThrowsWhenInsufficientBalance() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        account.setBalance(new BigDecimal("10.00"));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        UUID referenceId = UUID.randomUUID();
        when(transactionLogRepository.findByAccountIdAndReferenceId(account.getId(), referenceId)).thenReturn(Optional.empty());
        when(transactionLogRepository.countByAccountIdAndDateRange(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(0L);
        when(transactionLogRepository.sumAmountByAccountIdAndDateRange(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(BigDecimal.ZERO);

        AccountTransactionRequest request = new AccountTransactionRequest(referenceId, AccountTransactionType.DEBIT, new BigDecimal("50.00"), "ATM");

        assertThatThrownBy(() -> accountService.applyTransaction(account.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");
        verify(accountMetrics, times(0)).incrementDebit();
    }

    @Test
    void duplicateReferenceSkipsReapplication() {
        UUID accountId = UUID.randomUUID();
        Account account = buildAccount(accountId, UUID.randomUUID());
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        UUID referenceId = UUID.randomUUID();
        when(transactionLogRepository.findByAccountIdAndReferenceId(accountId, referenceId))
                .thenReturn(Optional.of(new AccountTransactionLog()));

        AccountTransactionRequest request = new AccountTransactionRequest(referenceId, AccountTransactionType.CREDIT, new BigDecimal("10.00"), "bonus");
        AccountResponse response = accountService.applyTransaction(accountId, request);

        assertThat(response.id()).isEqualTo(accountId);
        verify(accountRepository, times(0)).save(any(Account.class));
    }

    @Test
    void getBalanceReturnsAccountBalance() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        account.setBalance(new BigDecimal("500.00"));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        BalanceResponse response = accountService.getBalance(account.getId());

        assertThat(response.accountId()).isEqualTo(account.getId());
        assertThat(response.accountNumber()).isEqualTo(account.getAccountNumber());
        assertThat(response.balance()).isEqualByComparingTo("500.00");
        assertThat(response.currency()).isEqualTo("USD");
    }

    @Test
    void getTransactionHistoryReturnsPagedTransactions() {
        UUID accountId = UUID.randomUUID();
        Account account = buildAccount(accountId, UUID.randomUUID());
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        AccountTransactionLog log1 = buildTransactionLog(accountId, UUID.randomUUID(), AccountTransactionType.CREDIT, new BigDecimal("100.00"));
        AccountTransactionLog log2 = buildTransactionLog(accountId, UUID.randomUUID(), AccountTransactionType.DEBIT, new BigDecimal("50.00"));
        PageImpl<AccountTransactionLog> page = new PageImpl<>(List.of(log1, log2));
        when(transactionLogRepository.findByAccountIdOrderByCreatedAtDesc(eq(accountId), any(Pageable.class))).thenReturn(page);

        PageResponse<TransactionHistoryResponse> response = accountService.getTransactionHistory(accountId, 0, 20);

        assertThat(response.items()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.items().get(0).type()).isEqualTo(AccountTransactionType.CREDIT);
        assertThat(response.items().get(1).type()).isEqualTo(AccountTransactionType.DEBIT);
    }

    @Test
    void updateAccountUpdatesTypeAndCurrency() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        UpdateAccountRequest request = new UpdateAccountRequest(AccountType.SAVINGS, "EUR");
        accountService.updateAccount(account.getId(), request);

        assertThat(account.getType()).isEqualTo(AccountType.SAVINGS);
        assertThat(account.getCurrency()).isEqualTo("EUR");
        verify(eventPublisher).publishAccountUpdated(account);
        verify(accountMetrics).incrementUpdated();
    }

    @Test
    void updateAccountSupportsPartialUpdates() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        AccountType originalType = account.getType(); // CHECKING
        String originalCurrency = account.getCurrency();
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        // Update only type (CHECKING to SAVINGS is allowed)
        UpdateAccountRequest request = new UpdateAccountRequest(AccountType.SAVINGS, null);
        accountService.updateAccount(account.getId(), request);

        assertThat(account.getType()).isEqualTo(AccountType.SAVINGS);
        assertThat(account.getCurrency()).isEqualTo(originalCurrency);

        // Reset and update only currency
        account.setType(originalType);
        UpdateAccountRequest request2 = new UpdateAccountRequest(null, "GBP");
        accountService.updateAccount(account.getId(), request2);

        assertThat(account.getType()).isEqualTo(originalType);
        assertThat(account.getCurrency()).isEqualTo("GBP");
    }

    @Test
    void applyTransactionThrowsOnClosedAccount() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        account.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        UUID referenceId = UUID.randomUUID();
        AccountTransactionRequest request = new AccountTransactionRequest(referenceId, AccountTransactionType.CREDIT, new BigDecimal("10.00"), "test");

        assertThatThrownBy(() -> accountService.applyTransaction(account.getId(), request))
                .isInstanceOf(InvalidAccountStatusException.class)
                .hasMessageContaining("closed account");
    }

    @Test
    void applyTransactionThrowsOnSuspendedAccount() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        account.setStatus(AccountStatus.SUSPENDED);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        UUID referenceId = UUID.randomUUID();
        AccountTransactionRequest request = new AccountTransactionRequest(referenceId, AccountTransactionType.CREDIT, new BigDecimal("10.00"), "test");

        assertThatThrownBy(() -> accountService.applyTransaction(account.getId(), request))
                .isInstanceOf(InvalidAccountStatusException.class)
                .hasMessageContaining("suspended account");
    }

    @Test
    void getBalanceThrowsWhenAccountNotFound() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getBalance(accountId))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void getTransactionHistoryThrowsWhenAccountNotFound() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getTransactionHistory(accountId, 0, 20))
                .isInstanceOf(AccountNotFoundException.class);
    }

    private Account buildAccount(UUID id, UUID customerId) {
        Account account = new Account();
        account.setId(id);
        account.setAccountNumber("ACC-" + id.toString().substring(0, 12));
        account.setCustomerId(customerId);
        account.setType(AccountType.CHECKING);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCurrency("USD");
        account.setBalance(new BigDecimal("100.00"));
        account.setOpenedAt(Instant.now());
        account.setUpdatedAt(Instant.now());
        return account;
    }

    private AccountTransactionLog buildTransactionLog(UUID accountId, UUID referenceId, AccountTransactionType type, BigDecimal amount) {
        AccountTransactionLog log = new AccountTransactionLog();
        log.setAccountId(accountId);
        log.setReferenceId(referenceId);
        log.setType(type);
        log.setAmount(amount);
        log.setResultingBalance(new BigDecimal("100.00"));
        log.setCreatedAt(Instant.now());
        return log;
    }

    @Test
    void deleteAccountPerformsSoftDelete() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        account.setBalance(BigDecimal.ZERO);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        accountService.deleteAccount(account.getId());

        assertThat(account.getDeletedAt()).isNotNull();
        verify(accountRepository).save(account);
        verify(accountRepository, times(0)).delete(any(Account.class));
    }

    @Test
    void deleteAccountThrowsWhenAccountHasBalance() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        account.setBalance(new BigDecimal("100.00"));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.deleteAccount(account.getId()))
                .isInstanceOf(AccountClosureException.class)
                .hasMessageContaining("non-zero balance");
    }

    @Test
    void updateStatusToClosedThrowsWhenAccountHasBalance() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        account.setBalance(new BigDecimal("50.00"));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(AccountStatus.CLOSED);

        assertThatThrownBy(() -> accountService.updateStatus(account.getId(), request))
                .isInstanceOf(AccountClosureException.class)
                .hasMessageContaining("non-zero balance");
    }

    @Test
    void updateStatusToClosedSucceedsWhenBalanceIsZero() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        account.setBalance(BigDecimal.ZERO);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(AccountStatus.CLOSED);
        accountService.updateStatus(account.getId(), request);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        verify(accountRepository).save(account);
    }

    @Test
    void applyTransactionThrowsWhenExceedingMaxTransactionAmount() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        account.setBalance(new BigDecimal("1000000.00"));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        UUID referenceId = UUID.randomUUID();
        when(transactionLogRepository.findByAccountIdAndReferenceId(account.getId(), referenceId)).thenReturn(Optional.empty());
        when(transactionLogRepository.countByAccountIdAndDateRange(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(0L);
        when(transactionLogRepository.sumAmountByAccountIdAndDateRange(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(BigDecimal.ZERO);
        when(accountLimits.getMaxTransactionAmount()).thenReturn(new BigDecimal("1000000.00"));

        AccountTransactionRequest request = new AccountTransactionRequest(
                referenceId, AccountTransactionType.CREDIT, new BigDecimal("1000001.00"), "test");

        assertThatThrownBy(() -> accountService.applyTransaction(account.getId(), request))
                .isInstanceOf(AccountLimitException.class)
                .hasMessageContaining("exceeds maximum allowed amount");
    }

    @Test
    void applyTransactionThrowsWhenExceedingMaxDailyTransactions() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        account.setBalance(new BigDecimal("1000.00"));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        UUID referenceId = UUID.randomUUID();
        when(transactionLogRepository.findByAccountIdAndReferenceId(account.getId(), referenceId)).thenReturn(Optional.empty());
        when(transactionLogRepository.countByAccountIdAndDateRange(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(100L);
        when(accountLimits.getMaxDailyTransactions()).thenReturn(100);

        AccountTransactionRequest request = new AccountTransactionRequest(
                referenceId, AccountTransactionType.CREDIT, new BigDecimal("10.00"), "test");

        assertThatThrownBy(() -> accountService.applyTransaction(account.getId(), request))
                .isInstanceOf(AccountLimitException.class)
                .hasMessageContaining("exceeds maximum allowed");
    }

    @Test
    void applyTransactionThrowsWhenExceedingMaxDailyTransactionAmount() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        account.setBalance(new BigDecimal("1000.00"));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        UUID referenceId = UUID.randomUUID();
        when(transactionLogRepository.findByAccountIdAndReferenceId(account.getId(), referenceId)).thenReturn(Optional.empty());
        when(transactionLogRepository.countByAccountIdAndDateRange(any(), any(), any())).thenReturn(1L);
        when(transactionLogRepository.sumAmountByAccountIdAndDateRange(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new BigDecimal("50000.00"));
        when(accountLimits.getMaxDailyTransactionAmount()).thenReturn(new BigDecimal("50000.00"));

        AccountTransactionRequest request = new AccountTransactionRequest(
                referenceId, AccountTransactionType.CREDIT, new BigDecimal("1.00"), "test");

        assertThatThrownBy(() -> accountService.applyTransaction(account.getId(), request))
                .isInstanceOf(AccountLimitException.class)
                .hasMessageContaining("exceeds maximum allowed");
    }

    @Test
    void applyTransactionThrowsWhenBalanceExceedsMaxBalance() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        account.setBalance(new BigDecimal("999999999.99"));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        UUID referenceId = UUID.randomUUID();
        when(transactionLogRepository.findByAccountIdAndReferenceId(account.getId(), referenceId)).thenReturn(Optional.empty());
        when(transactionLogRepository.countByAccountIdAndDateRange(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(0L);
        when(transactionLogRepository.sumAmountByAccountIdAndDateRange(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(BigDecimal.ZERO);
        when(accountLimits.getMaxBalance()).thenReturn(new BigDecimal("999999999.99"));

        AccountTransactionRequest request = new AccountTransactionRequest(
                referenceId, AccountTransactionType.CREDIT, new BigDecimal("1.00"), "test");

        assertThatThrownBy(() -> accountService.applyTransaction(account.getId(), request))
                .isInstanceOf(AccountLimitException.class)
                .hasMessageContaining("exceeds maximum allowed balance");
    }

    @Test
    void applyTransactionThrowsWhenBalanceBelowMinBalance() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        account.setBalance(new BigDecimal("10.00"));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        UUID referenceId = UUID.randomUUID();
        when(transactionLogRepository.findByAccountIdAndReferenceId(account.getId(), referenceId)).thenReturn(Optional.empty());
        when(transactionLogRepository.countByAccountIdAndDateRange(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(0L);
        when(transactionLogRepository.sumAmountByAccountIdAndDateRange(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(BigDecimal.ZERO);
        when(accountLimits.getMinBalance()).thenReturn(new BigDecimal("50.00"));

        AccountTransactionRequest request = new AccountTransactionRequest(
                referenceId, AccountTransactionType.DEBIT, new BigDecimal("5.00"), "test");

        assertThatThrownBy(() -> accountService.applyTransaction(account.getId(), request))
                .isInstanceOf(AccountLimitException.class)
                .hasMessageContaining("below minimum required balance");
    }

    @Test
    void createAccountValidatesCustomerExists() {
        UUID customerId = UUID.randomUUID();
        CreateAccountRequest request = new CreateAccountRequest(customerId, AccountType.CHECKING, "USD", BigDecimal.valueOf(100));
        when(accountNumberGenerator.generate()).thenReturn("ACC-123456789012");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setOpenedAt(Instant.now());
            account.setUpdatedAt(Instant.now());
            return account;
        });

        accountService.createAccount(request);

        verify(customerValidationService).validateCustomerExists(customerId);
    }

    @Test
    void applyTransactionRecordsMetrics() {
        Account account = buildAccount(UUID.randomUUID(), UUID.randomUUID());
        account.setBalance(new BigDecimal("200.00"));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        UUID referenceId = UUID.randomUUID();
        when(transactionLogRepository.findByAccountIdAndReferenceId(account.getId(), referenceId)).thenReturn(Optional.empty());
        when(transactionLogRepository.countByAccountIdAndDateRange(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(0L);
        when(transactionLogRepository.sumAmountByAccountIdAndDateRange(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(BigDecimal.ZERO);

        AccountTransactionRequest request = new AccountTransactionRequest(referenceId, AccountTransactionType.CREDIT, new BigDecimal("50.00"), "bonus");
        accountService.applyTransaction(account.getId(), request);

        verify(accountMetrics).incrementCredit();
        verify(accountMetrics).recordTransactionAmount(any(BigDecimal.class), eq(AccountTransactionType.CREDIT));
        verify(accountMetrics).recordBalance(any(BigDecimal.class));
    }
}

