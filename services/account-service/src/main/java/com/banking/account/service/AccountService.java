package com.banking.account.service;

import com.banking.account.domain.Account;
import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountTransactionLog;
import com.banking.account.domain.AccountTransactionType;
import com.banking.account.domain.AccountType;
import com.banking.account.metrics.AccountMetrics;
import com.banking.account.security.TenantAccessEvaluator;
import com.banking.account.messaging.AccountEventPublisher;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.AccountTransactionLogRepository;
import com.banking.account.web.AccountMapper;
import com.banking.account.web.dto.AccountResponse;
import com.banking.account.web.dto.AccountTransactionRequest;
import com.banking.account.web.dto.BalanceResponse;
import com.banking.account.web.dto.BulkAccountResponse;
import com.banking.account.web.dto.BulkCreateAccountRequest;
import com.banking.account.web.dto.BulkTransactionRequest;
import com.banking.account.web.dto.BulkUpdateStatusRequest;
import com.banking.account.web.dto.CreateAccountRequest;
import com.banking.account.web.dto.PageResponse;
import com.banking.account.web.dto.TransactionHistoryResponse;
import com.banking.account.web.dto.UpdateAccountRequest;
import com.banking.account.web.dto.UpdateAccountStatusRequest;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AccountService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AccountRepository accountRepository;
    private final AccountTransactionLogRepository transactionLogRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final AccountEventPublisher eventPublisher;
    private final AccountMetrics accountMetrics;
    private final CustomerValidationService customerValidationService;
    private final CurrencyValidationService currencyValidationService;
    private final com.banking.account.config.AccountLimitsProperties accountLimits;
    private final com.banking.account.messaging.TransactionAuditLogger auditLogger;

    private final TenantAccessEvaluator tenantAccessEvaluator;

    public AccountService(
            AccountRepository accountRepository,
            AccountTransactionLogRepository transactionLogRepository,
            AccountNumberGenerator accountNumberGenerator,
            AccountEventPublisher eventPublisher,
            AccountMetrics accountMetrics,
            CustomerValidationService customerValidationService,
            CurrencyValidationService currencyValidationService,
            com.banking.account.config.AccountLimitsProperties accountLimits,
            com.banking.account.messaging.TransactionAuditLogger auditLogger,
            TenantAccessEvaluator tenantAccessEvaluator
    ) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.eventPublisher = eventPublisher;
        this.accountMetrics = accountMetrics;
        this.customerValidationService = customerValidationService;
        this.currencyValidationService = currencyValidationService;
        this.accountLimits = accountLimits;
        this.auditLogger = auditLogger;
        this.tenantAccessEvaluator = tenantAccessEvaluator;
    }

    public AccountResponse createAccount(CreateAccountRequest request) {
        // Validate customer exists
        customerValidationService.validateCustomerExists(request.customerId());

        // Validate currency
        currencyValidationService.validateCurrency(request.currency());

        // Validate account type (no combination validation needed on creation)
        validateAccountType(request.type(), null);

        // Validate initial deposit
        BigDecimal normalizedDeposit = normalizeAmount(request.initialDeposit());
        validateBalanceLimits(normalizedDeposit, null);

        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setAccountNumber(accountNumberGenerator.generate());
        account.setCustomerId(request.customerId());
        account.setType(request.type());
        account.setStatus(AccountStatus.ACTIVE);
        account.setCurrency(request.currency().toUpperCase());
        account.setBalance(normalizedDeposit);

        Account saved = accountRepository.save(account);
        eventPublisher.publishAccountCreated(saved);
        accountMetrics.incrementCreated();
        accountMetrics.recordBalance(saved.getBalance());
        return AccountMapper.toResponse(saved);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    @org.springframework.cache.annotation.Cacheable(value = com.banking.account.config.CacheConfig.ACCOUNT_CACHE, key = "#accountId")
    public AccountResponse getAccount(UUID accountId) {
        return AccountMapper.toResponse(loadSecuredAccount(accountId));
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public PageResponse<AccountResponse> listAccounts(UUID customerId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        Page<Account> accountPage = customerId == null
                ? accountRepository.findAll(pageable)
                : accountRepository.findByCustomerId(customerId, pageable);
        return PageResponse.from(accountPage.map(AccountMapper::toResponse));
    }

    public AccountResponse updateStatus(UUID accountId, UpdateAccountStatusRequest request) {
        Account account = loadSecuredAccount(accountId);
        AccountStatus oldStatus = account.getStatus();
        
        // Validate account closure
        if (request.status() == AccountStatus.CLOSED) {
            if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                throw AccountClosureException.hasBalance(accountId, account.getBalance());
            }
        }
        
        account.setStatus(request.status());
        Account updated = accountRepository.save(account);
        eventPublisher.publishAccountUpdated(updated);
        accountMetrics.incrementUpdated();
        accountMetrics.updateAccountStatus(oldStatus, request.status());
        evictAccountCache(updated.getId(), updated.getAccountNumber());
        return AccountMapper.toResponse(updated);
    }

    public AccountResponse applyTransaction(UUID accountId, AccountTransactionRequest request) {
        // Validate idempotency key
        validateIdempotencyKey(request.referenceId());
        
        Account account = loadSecuredAccount(accountId);
        validateAccountStatusForTransaction(account);
        return transactionLogRepository.findByAccountIdAndReferenceId(accountId, request.referenceId())
                .map(log -> AccountMapper.toResponse(loadAccount(accountId)))
                .orElseGet(() -> processTransaction(accountId, request));
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public BalanceResponse getBalance(UUID accountId) {
        Account account = loadSecuredAccount(accountId);
        return AccountMapper.toBalanceResponse(account);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public PageResponse<TransactionHistoryResponse> getTransactionHistory(UUID accountId, int page, int size) {
        loadSecuredAccount(accountId); // Validate account exists and tenant access
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        return PageResponse.from(
                transactionLogRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                        .map(AccountMapper::toTransactionHistoryResponse)
        );
    }

    public AccountResponse updateAccount(UUID accountId, UpdateAccountRequest request) {
        Account account = loadSecuredAccount(accountId);
        
        if (request.type() != null) {
            // Validate account type combination
            validateAccountType(request.type(), account.getType());
            account.setType(request.type());
        }
        
        if (request.currency() != null) {
            // Validate currency
            currencyValidationService.validateCurrency(request.currency());
            account.setCurrency(request.currency().toUpperCase());
        }
        
        Account updated = accountRepository.save(account);
        eventPublisher.publishAccountUpdated(updated);
        accountMetrics.incrementUpdated();
        evictAccountCache(updated.getId(), updated.getAccountNumber());
        return AccountMapper.toResponse(updated);
    }

    public void deleteAccount(UUID accountId) {
        Account account = loadSecuredAccount(accountId);
        
        // Validate account can be deleted
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw AccountClosureException.hasBalance(accountId, account.getBalance());
        }
        
        // Soft delete
        account.setDeletedAt(Instant.now());
        accountRepository.save(account);
        // Invalidate cache on delete
        evictAccountCache(account.getId(), account.getAccountNumber());
    }

    private Account loadAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private Account loadSecuredAccount(UUID accountId) {
        Account account = loadAccount(accountId);
        tenantAccessEvaluator.assertCanAccessAccount(account);
        return account;
    }

    private void validateAccountStatusForTransaction(Account account) {
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidAccountStatusException(account.getId(), "Cannot process transaction on closed account");
        }
        if (account.getStatus() == AccountStatus.SUSPENDED) {
            throw new InvalidAccountStatusException(account.getId(), "Cannot process transaction on suspended account");
        }
    }

    private void validateIdempotencyKey(UUID referenceId) {
        if (referenceId == null) {
            throw InvalidIdempotencyKeyException.nullKey();
        }
        // UUID validation is already done by the framework, but we can add additional checks here
        // For example, check if it's a well-formed UUID (already validated by @NotNull)
    }

    private void validateAccountType(AccountType newType, AccountType currentType) {
        // Business rule: Cannot change from CREDIT to CHECKING or SAVINGS
        // This prevents customers from converting credit accounts to deposit accounts
        if (currentType == AccountType.CREDIT && 
            (newType == AccountType.CHECKING || newType == AccountType.SAVINGS)) {
            throw InvalidAccountTypeException.invalidCombination(currentType, newType);
        }
        
        // Business rule: Cannot change from CHECKING/SAVINGS to CREDIT
        // Credit accounts require separate approval process
        if ((currentType == AccountType.CHECKING || currentType == AccountType.SAVINGS) && 
            newType == AccountType.CREDIT) {
            throw InvalidAccountTypeException.invalidCombination(currentType, newType);
        }
        
        // Allow: CHECKING <-> SAVINGS (both are deposit accounts)
        // Allow: Same type (no-op)
        // All other combinations are allowed
    }

    private void validateBalanceLimits(BigDecimal balance, UUID accountId) {
        if (balance.compareTo(accountLimits.getMinBalance()) < 0) {
            if (accountId != null) {
                throw AccountLimitException.minBalanceViolation(accountId, balance, accountLimits.getMinBalance());
            } else {
                throw new AccountLimitException(
                        String.format("Balance %.2f is below minimum required balance %.2f", 
                                balance, accountLimits.getMinBalance())
                );
            }
        }
        if (balance.compareTo(accountLimits.getMaxBalance()) > 0) {
            if (accountId != null) {
                throw AccountLimitException.maxBalanceViolation(accountId, balance, accountLimits.getMaxBalance());
            } else {
                throw new AccountLimitException(
                        String.format("Balance %.2f exceeds maximum allowed balance %.2f", 
                                balance, accountLimits.getMaxBalance())
                );
            }
        }
    }

    private void validateDailyTransactionLimits(UUID accountId, BigDecimal amount) {
        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        // Use optimized queries instead of loading all transactions
        long transactionCount = transactionLogRepository.countByAccountIdAndDateRange(accountId, startOfDay, endOfDay);

        if (transactionCount >= accountLimits.getMaxDailyTransactions()) {
            throw AccountLimitException.maxDailyTransactionsViolation((int) transactionCount, accountLimits.getMaxDailyTransactions());
        }

        BigDecimal dailyAmount = transactionLogRepository.sumAmountByAccountIdAndDateRange(accountId, startOfDay, endOfDay);
        BigDecimal newDailyAmount = dailyAmount.add(amount);
        if (newDailyAmount.compareTo(accountLimits.getMaxDailyTransactionAmount()) > 0) {
            throw AccountLimitException.maxDailyTransactionAmountViolation(newDailyAmount, accountLimits.getMaxDailyTransactionAmount());
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private AccountResponse processTransaction(UUID accountId, AccountTransactionRequest request) {
        io.micrometer.core.instrument.Timer.Sample timerSample = accountMetrics.startTransactionTimer();
        try {
            Account account = loadAccount(accountId);
            BigDecimal normalizedAmount = normalizeAmount(request.amount());
            
            // Validate transaction amount limits
            if (normalizedAmount.compareTo(accountLimits.getMaxTransactionAmount()) > 0) {
                throw AccountLimitException.maxTransactionAmountViolation(normalizedAmount, accountLimits.getMaxTransactionAmount());
            }
            
            // Validate daily transaction limits
            validateDailyTransactionLimits(accountId, normalizedAmount);
            
            if (request.type() == AccountTransactionType.DEBIT) {
                if (account.getBalance().compareTo(normalizedAmount) < 0) {
                    throw new IllegalArgumentException("Insufficient balance for debit transaction");
                }
                BigDecimal newBalance = account.getBalance().subtract(normalizedAmount);
                validateBalanceLimits(newBalance, accountId);
                account.setBalance(newBalance);
                accountMetrics.incrementDebit();
                accountMetrics.recordTransactionAmount(normalizedAmount, AccountTransactionType.DEBIT);
            } else {
                BigDecimal newBalance = account.getBalance().add(normalizedAmount);
                validateBalanceLimits(newBalance, accountId);
                account.setBalance(newBalance);
                accountMetrics.incrementCredit();
                accountMetrics.recordTransactionAmount(normalizedAmount, AccountTransactionType.CREDIT);
            }

            Account updated = accountRepository.save(account);
            recordTransactionLog(updated, request, normalizedAmount);
            eventPublisher.publishAccountUpdated(updated);
            accountMetrics.recordBalance(updated.getBalance());
            accountMetrics.recordTransactionTime(timerSample);
            return AccountMapper.toResponse(updated);
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
            throw new ConcurrentAccountUpdateException(accountId, ex);
        } catch (DataIntegrityViolationException duplicateReference) {
            return AccountMapper.toResponse(loadAccount(accountId));
        }
    }

    private void recordTransactionLog(Account account, AccountTransactionRequest request, BigDecimal amount) {
        AccountTransactionLog log = new AccountTransactionLog();
        log.setAccountId(account.getId());
        log.setReferenceId(request.referenceId());
        log.setType(request.type());
        log.setAmount(amount);
        log.setResultingBalance(account.getBalance());
        log.setCreatedAt(Instant.now());
        transactionLogRepository.save(log);
        
        // Audit logging
        auditLogger.logTransaction(account.getId(), request.referenceId(), request.type(), 
                amount, account.getBalance(), request.description());
    }

    @CacheEvict(value = com.banking.account.config.CacheConfig.ACCOUNT_CACHE, key = "#accountId")
    private void evictAccountCache(UUID accountId, String accountNumber) {
        // Cache eviction is handled by @CacheEvict annotation
        // This method triggers cache eviction when called
    }

    /**
     * Bulk create accounts for a customer.
     */
    public BulkAccountResponse bulkCreateAccounts(BulkCreateAccountRequest request) {
        List<AccountResponse> createdAccounts = new ArrayList<>();
        List<BulkAccountResponse.BulkOperationError> errors = new ArrayList<>();

        for (int i = 0; i < request.accounts().size(); i++) {
            BulkCreateAccountRequest.AccountCreationItem item = request.accounts().get(i);
            try {
                CreateAccountRequest createRequest = new CreateAccountRequest(
                        request.customerId(),
                        item.type(),
                        item.currency(),
                        item.initialDeposit()
                );
                AccountResponse account = createAccount(createRequest);
                createdAccounts.add(account);
            } catch (Exception ex) {
                errors.add(new BulkAccountResponse.BulkOperationError(
                        i,
                        ex.getClass().getSimpleName(),
                        ex.getMessage()
                ));
            }
        }

        return new BulkAccountResponse(
                request.accounts().size(),
                createdAccounts.size(),
                errors.size(),
                createdAccounts,
                errors
        );
    }

    /**
     * Bulk update account statuses.
     */
    public BulkAccountResponse bulkUpdateStatus(BulkUpdateStatusRequest request) {
        List<AccountResponse> updatedAccounts = new ArrayList<>();
        List<BulkAccountResponse.BulkOperationError> errors = new ArrayList<>();

        for (int i = 0; i < request.accountIds().size(); i++) {
            UUID accountId = request.accountIds().get(i);
            try {
                UpdateAccountStatusRequest statusRequest = new UpdateAccountStatusRequest(request.status());
                AccountResponse account = updateStatus(accountId, statusRequest);
                updatedAccounts.add(account);
            } catch (Exception ex) {
                errors.add(new BulkAccountResponse.BulkOperationError(
                        i,
                        ex.getClass().getSimpleName(),
                        ex.getMessage()
                ));
            }
        }

        return new BulkAccountResponse(
                request.accountIds().size(),
                updatedAccounts.size(),
                errors.size(),
                updatedAccounts,
                errors
        );
    }

    /**
     * Bulk process transactions.
     */
    public BulkAccountResponse bulkProcessTransactions(BulkTransactionRequest request) {
        List<AccountResponse> updatedAccounts = new ArrayList<>();
        List<BulkAccountResponse.BulkOperationError> errors = new ArrayList<>();

        for (int i = 0; i < request.transactions().size(); i++) {
            BulkTransactionRequest.TransactionItem item = request.transactions().get(i);
            try {
                AccountTransactionRequest transactionRequest = new AccountTransactionRequest(
                        item.referenceId(),
                        item.type(),
                        item.amount(),
                        item.description()
                );
                AccountResponse account = applyTransaction(item.accountId(), transactionRequest);
                updatedAccounts.add(account);
            } catch (Exception ex) {
                errors.add(new BulkAccountResponse.BulkOperationError(
                        i,
                        ex.getClass().getSimpleName(),
                        ex.getMessage()
                ));
            }
        }

        return new BulkAccountResponse(
                request.transactions().size(),
                updatedAccounts.size(),
                errors.size(),
                updatedAccounts,
                errors
        );
    }
}

