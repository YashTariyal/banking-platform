package com.banking.account.service;

import com.banking.account.config.AccountLimitsProperties;
import com.banking.account.domain.Account;
import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountTransactionType;
import com.banking.account.domain.AccountType;
import com.banking.account.messaging.AccountEventPublisher;
import com.banking.account.messaging.TransactionAuditLogger;
import com.banking.account.metrics.AccountMetrics;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.AccountTransactionLogRepository;
import com.banking.account.web.AccountMapper;
import com.banking.account.web.dto.BulkAccountResponse;
import com.banking.account.web.dto.BulkCreateAccountRequest;
import com.banking.account.web.dto.BulkTransactionRequest;
import com.banking.account.web.dto.BulkUpdateStatusRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BulkOperationsTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountTransactionLogRepository transactionLogRepository;
    @Mock
    private com.banking.account.service.AccountNumberGenerator accountNumberGenerator;
    @Mock
    private AccountEventPublisher eventPublisher;
    @Mock
    private AccountMetrics accountMetrics;
    @Mock
    private CustomerValidationService customerValidationService;
    private CurrencyValidationService currencyValidationService;
    @Mock
    private AccountLimitsProperties accountLimits;
    @Mock
    private TransactionAuditLogger auditLogger;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        when(accountLimits.getMinBalance()).thenReturn(BigDecimal.ZERO);
        when(accountLimits.getMaxBalance()).thenReturn(new BigDecimal("999999999.99"));
        when(accountLimits.getMaxTransactionAmount()).thenReturn(new BigDecimal("1000000.00"));
        when(accountLimits.getMaxDailyTransactions()).thenReturn(100);
        when(accountLimits.getMaxDailyTransactionAmount()).thenReturn(new BigDecimal("50000.00"));
        currencyValidationService = new CurrencyValidationService();

        accountService = new AccountService(
                accountRepository,
                transactionLogRepository,
                accountNumberGenerator,
                eventPublisher,
                accountMetrics,
                customerValidationService,
                currencyValidationService,
                accountLimits,
                auditLogger
        );
    }

    @Test
    void bulkCreateAccountsSuccess() {
        UUID customerId = UUID.randomUUID();
        BulkCreateAccountRequest request = new BulkCreateAccountRequest(
                customerId,
                List.of(
                        new BulkCreateAccountRequest.AccountCreationItem(AccountType.CHECKING, "USD", BigDecimal.valueOf(100)),
                        new BulkCreateAccountRequest.AccountCreationItem(AccountType.SAVINGS, "USD", BigDecimal.valueOf(200))
                )
        );

        when(accountNumberGenerator.generate()).thenReturn("ACC-123456789012", "ACC-123456789013");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setOpenedAt(Instant.now());
            account.setUpdatedAt(Instant.now());
            return account;
        });
        BulkAccountResponse response = accountService.bulkCreateAccounts(request);

        assertThat(response.totalRequested()).isEqualTo(2);
        assertThat(response.successful()).isEqualTo(2);
        assertThat(response.failed()).isEqualTo(0);
        assertThat(response.createdAccounts()).hasSize(2);
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void bulkCreateAccountsPartialFailure() {
        UUID customerId = UUID.randomUUID();
        BulkCreateAccountRequest request = new BulkCreateAccountRequest(
                customerId,
                List.of(
                        new BulkCreateAccountRequest.AccountCreationItem(AccountType.CHECKING, "USD", BigDecimal.valueOf(100)),
                        new BulkCreateAccountRequest.AccountCreationItem(AccountType.CHECKING, "INVALID", BigDecimal.valueOf(200))
                )
        );

        when(accountNumberGenerator.generate()).thenReturn("ACC-123456789012");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setOpenedAt(Instant.now());
            account.setUpdatedAt(Instant.now());
            return account;
        });

        BulkAccountResponse response = accountService.bulkCreateAccounts(request);

        assertThat(response.totalRequested()).isEqualTo(2);
        assertThat(response.successful()).isEqualTo(1);
        assertThat(response.failed()).isEqualTo(1);
        assertThat(response.createdAccounts()).hasSize(1);
        assertThat(response.errors()).hasSize(1);
    }

    @Test
    void bulkUpdateStatusSuccess() {
        UUID accountId1 = UUID.randomUUID();
        UUID accountId2 = UUID.randomUUID();
        BulkUpdateStatusRequest request = new BulkUpdateStatusRequest(
                List.of(accountId1, accountId2),
                AccountStatus.SUSPENDED
        );

        Account account1 = buildAccount(accountId1, UUID.randomUUID());
        Account account2 = buildAccount(accountId2, UUID.randomUUID());
        account1.setBalance(BigDecimal.ZERO);
        account2.setBalance(BigDecimal.ZERO);

        when(accountRepository.findById(accountId1)).thenReturn(Optional.of(account1));
        when(accountRepository.findById(accountId2)).thenReturn(Optional.of(account2));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BulkAccountResponse response = accountService.bulkUpdateStatus(request);

        assertThat(response.totalRequested()).isEqualTo(2);
        assertThat(response.successful()).isEqualTo(2);
        assertThat(response.failed()).isEqualTo(0);
        assertThat(response.createdAccounts()).hasSize(2);
    }

    @Test
    void bulkProcessTransactionsSuccess() {
        UUID accountId = UUID.randomUUID();
        UUID referenceId1 = UUID.randomUUID();
        UUID referenceId2 = UUID.randomUUID();
        
        Account account = buildAccount(accountId, UUID.randomUUID());
        account.setBalance(new BigDecimal("1000.00"));

        BulkTransactionRequest request = new BulkTransactionRequest(
                List.of(
                        new BulkTransactionRequest.TransactionItem(accountId, referenceId1, AccountTransactionType.CREDIT, BigDecimal.valueOf(100), "test1"),
                        new BulkTransactionRequest.TransactionItem(accountId, referenceId2, AccountTransactionType.CREDIT, BigDecimal.valueOf(200), "test2")
                )
        );

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionLogRepository.findByAccountIdAndReferenceId(any(), any())).thenReturn(Optional.empty());
        when(transactionLogRepository.countByAccountIdAndDateRange(any(), any(), any())).thenReturn(0L);
        when(transactionLogRepository.sumAmountByAccountIdAndDateRange(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        BulkAccountResponse response = accountService.bulkProcessTransactions(request);

        assertThat(response.totalRequested()).isEqualTo(2);
        assertThat(response.successful()).isEqualTo(2);
        assertThat(response.failed()).isEqualTo(0);
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
}

