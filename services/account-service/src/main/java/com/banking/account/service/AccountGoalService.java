package com.banking.account.service;

import com.banking.account.config.AccountGoalProperties;
import com.banking.account.domain.Account;
import com.banking.account.domain.AccountGoal;
import com.banking.account.domain.AccountGoalCadence;
import com.banking.account.domain.AccountGoalContribution;
import com.banking.account.domain.AccountGoalContributionSource;
import com.banking.account.domain.AccountGoalStatus;
import com.banking.account.domain.AccountTransactionType;
import com.banking.account.metrics.AccountMetrics;
import com.banking.account.repository.AccountGoalContributionRepository;
import com.banking.account.repository.AccountGoalRepository;
import com.banking.account.repository.AccountRepository;
import com.banking.account.web.AccountGoalMapper;
import com.banking.account.web.dto.AccountGoalResponse;
import com.banking.account.web.dto.CreateAccountGoalRequest;
import com.banking.account.web.dto.GoalContributionRequest;
import com.banking.account.web.dto.PageResponse;
import com.banking.account.web.dto.UpdateAccountGoalRequest;
import com.banking.account.web.dto.AccountTransactionRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.banking.account.security.TenantAccessEvaluator;

@Service
@Transactional
public class AccountGoalService {

    private static final Logger log = LoggerFactory.getLogger(AccountGoalService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final AccountGoalRepository goalRepository;
    private final AccountGoalContributionRepository contributionRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final AccountGoalProperties goalProperties;
    private final AccountMetrics accountMetrics;
    private final TenantAccessEvaluator tenantAccessEvaluator;

    public AccountGoalService(
            AccountGoalRepository goalRepository,
            AccountGoalContributionRepository contributionRepository,
            AccountRepository accountRepository,
            AccountService accountService,
            AccountGoalProperties goalProperties,
            AccountMetrics accountMetrics,
            TenantAccessEvaluator tenantAccessEvaluator
    ) {
        this.goalRepository = goalRepository;
        this.contributionRepository = contributionRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.goalProperties = goalProperties;
        this.accountMetrics = accountMetrics;
        this.tenantAccessEvaluator = tenantAccessEvaluator;
    }

    public AccountGoalResponse createGoal(UUID accountId, CreateAccountGoalRequest request) {
        Account account = loadSecuredAccount(accountId);
        BigDecimal normalizedTarget = normalize(request.targetAmount());
        validateGoalAmount(normalizedTarget);

        Instant now = Instant.now();
        AccountGoal goal = new AccountGoal();
        goal.setId(UUID.randomUUID());
        goal.setAccountId(account.getId());
        goal.setName(request.name());
        goal.setDescription(request.description());
        goal.setTargetAmount(normalizedTarget);
        goal.setCurrentAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        goal.setDueDate(request.dueDate());
        goal.setStatus(AccountGoalStatus.ACTIVE);
        boolean autoSweepEnabled = Boolean.TRUE.equals(request.autoSweepEnabled());
        goal.setAutoSweepEnabled(autoSweepEnabled);
        AccountGoalCadence cadence = resolveCadence(request.autoSweepCadence());
        if (autoSweepEnabled) {
            goal.setAutoSweepAmount(determineAutoSweepAmount(request.autoSweepAmount()));
            goal.setAutoSweepCadence(cadence);
            goal.setNextSweepAt(cadence.nextExecutionFrom(now));
        } else {
            goal.setAutoSweepAmount(null);
            goal.setAutoSweepCadence(cadence);
            goal.setNextSweepAt(null);
        }

        AccountGoal persisted = goalRepository.save(goal);
        accountMetrics.incrementGoalCreated();
        return AccountGoalMapper.toResponse(persisted);
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountGoalResponse> listGoals(UUID accountId, int page, int size) {
        loadSecuredAccount(accountId);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(0, page), safeSize);
        Page<AccountGoal> goals = goalRepository.findByAccountId(accountId, pageable);
        return PageResponse.from(goals.map(AccountGoalMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public AccountGoalResponse getGoal(UUID accountId, UUID goalId) {
        AccountGoal goal = loadGoal(accountId, goalId);
        return AccountGoalMapper.toResponse(goal);
    }

    public AccountGoalResponse updateGoal(UUID accountId, UUID goalId, UpdateAccountGoalRequest request) {
        AccountGoal goal = loadGoal(accountId, goalId);

        if (request.name() != null) {
            goal.setName(request.name());
        }
        if (request.description() != null) {
            goal.setDescription(request.description());
        }
        if (request.targetAmount() != null) {
            BigDecimal normalizedTarget = normalize(request.targetAmount());
            validateGoalAmount(normalizedTarget);
            if (normalizedTarget.compareTo(goal.getCurrentAmount()) < 0) {
                throw new AccountGoalValidationException("Target amount cannot be less than contributed amount");
            }
            goal.setTargetAmount(normalizedTarget);
        }
        if (request.dueDate() != null) {
            goal.setDueDate(request.dueDate());
        }
        boolean autoSweepSettingsChanged = false;
        if (request.autoSweepEnabled() != null) {
            goal.setAutoSweepEnabled(request.autoSweepEnabled());
            autoSweepSettingsChanged = true;
        }
        if (request.autoSweepAmount() != null) {
            goal.setAutoSweepAmount(determineAutoSweepAmount(request.autoSweepAmount()));
        }
        if (request.autoSweepCadence() != null) {
            goal.setAutoSweepCadence(resolveCadence(request.autoSweepCadence()));
            autoSweepSettingsChanged = true;
        }
        if (request.status() != null) {
            goal.setStatus(request.status());
            if (request.status() == AccountGoalStatus.CANCELLED) {
                goal.setAutoSweepEnabled(false);
                autoSweepSettingsChanged = true;
            }
        }

        if (autoSweepSettingsChanged) {
            if (goal.isAutoSweepEnabled()) {
                AccountGoalCadence cadence = resolveCadence(goal.getAutoSweepCadence());
                goal.setNextSweepAt(cadence.nextExecutionFrom(Instant.now()));
            } else {
                goal.setNextSweepAt(null);
            }
        }

        AccountGoal saved = goalRepository.save(goal);
        if (saved.getStatus() == AccountGoalStatus.COMPLETED) {
            accountMetrics.incrementGoalCompleted();
        }
        return AccountGoalMapper.toResponse(saved);
    }

    public AccountGoalResponse contributeToGoal(UUID accountId, UUID goalId, GoalContributionRequest request) {
        AccountGoal goal = loadGoal(accountId, goalId);
        ensureGoalIsActive(goal);
        BigDecimal normalizedAmount = normalize(request.amount());
        UUID referenceId = Optional.ofNullable(request.referenceId()).orElseGet(UUID::randomUUID);
        return applyContribution(goal, normalizedAmount, referenceId, request.description(), AccountGoalContributionSource.MANUAL, Instant.now());
    }

    public void processAutoSweep(UUID goalId, Instant now) {
        Optional<AccountGoal> optionalGoal = goalRepository.findById(goalId);
        if (optionalGoal.isEmpty()) {
            return;
        }
        AccountGoal goal = optionalGoal.get();
        if (!goal.isAutoSweepEnabled() || goal.getStatus() != AccountGoalStatus.ACTIVE) {
            return;
        }

        AccountGoalCadence cadence = resolveCadence(goal.getAutoSweepCadence());
        if (!isDueForSweep(goal, cadence, now)) {
            return;
        }

        Account account = loadSecuredAccount(goal.getAccountId());
        BigDecimal available = account.getBalance().subtract(goalProperties.getAutoSweep().getMinBalanceBuffer());
        if (available.compareTo(goalProperties.getAutoSweep().getMinContributionAmount()) < 0) {
            return;
        }

        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount());
        if (remaining.compareTo(goalProperties.getAutoSweep().getMinContributionAmount()) < 0) {
            markCompleted(goal, now);
            return;
        }

        BigDecimal desiredAmount = goal.getAutoSweepAmount() != null
                ? goal.getAutoSweepAmount()
                : goalProperties.getAutoSweep().getDefaultContributionAmount();

        BigDecimal contribution = normalize(desiredAmount.min(remaining).min(available)
                .min(goalProperties.getAutoSweep().getMaxContributionAmount()));

        if (contribution.compareTo(goalProperties.getAutoSweep().getMinContributionAmount()) < 0) {
            return;
        }

        UUID referenceId = generateAutoSweepReference(goal, cadence, now);
        BigDecimal before = goal.getCurrentAmount();
        applyContribution(goal, contribution, referenceId,
                "Automated sweep contribution", AccountGoalContributionSource.AUTO_SWEEP, now);

        if (goal.getCurrentAmount().compareTo(before) > 0) {
            goal.setLastSweepAt(now);
            goal.setNextSweepAt(cadence.nextExecutionFrom(now));
            goalRepository.save(goal);
            log.debug("Auto-sweep applied to goal {} for amount {}", goal.getId(), contribution);
        }
    }

    private AccountGoalResponse applyContribution(
            AccountGoal goal,
            BigDecimal amount,
            UUID referenceId,
            String description,
            AccountGoalContributionSource source,
            Instant timestamp
    ) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AccountGoalContributionException("Contribution amount must be positive");
        }

        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount());
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            markCompleted(goal, timestamp);
            return AccountGoalMapper.toResponse(goal);
        }

        BigDecimal contributionAmount = amount.min(remaining);
        try {
            AccountTransactionRequest transactionRequest = new AccountTransactionRequest(
                    referenceId,
                    AccountTransactionType.DEBIT,
                    contributionAmount,
                    description != null ? description : "Goal contribution"
            );
            accountService.applyTransaction(goal.getAccountId(), transactionRequest);
        } catch (IllegalArgumentException ex) {
            throw new AccountGoalContributionException("Unable to contribute to goal: " + ex.getMessage());
        }

        AccountGoalContribution contribution = new AccountGoalContribution();
        contribution.setId(UUID.randomUUID());
        contribution.setGoalId(goal.getId());
        contribution.setAccountId(goal.getAccountId());
        contribution.setAmount(contributionAmount);
        contribution.setSource(source);
        contribution.setDescription(description);
        contribution.setReferenceId(referenceId);
        contribution.setCreatedAt(timestamp);
        try {
            contributionRepository.save(contribution);
        } catch (DataIntegrityViolationException duplicateReference) {
            log.debug("Duplicate contribution reference detected for account {} and reference {}",
                    goal.getAccountId(), referenceId);
            return AccountGoalMapper.toResponse(goal);
        }

        goal.setCurrentAmount(goal.getCurrentAmount().add(contributionAmount));
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            markCompleted(goal, timestamp);
        }
        AccountGoal updated = goalRepository.save(goal);

        accountMetrics.recordGoalContribution(contributionAmount, source);
        if (updated.getStatus() == AccountGoalStatus.COMPLETED) {
            accountMetrics.incrementGoalCompleted();
        }
        return AccountGoalMapper.toResponse(updated);
    }

    private void markCompleted(AccountGoal goal, Instant timestamp) {
        goal.setStatus(AccountGoalStatus.COMPLETED);
        goal.setAutoSweepEnabled(false);
        goal.setCompletedAt(timestamp);
        goal.setNextSweepAt(null);
    }

    private boolean isDueForSweep(AccountGoal goal, AccountGoalCadence cadence, Instant now) {
        if (goal.getNextSweepAt() == null) {
            return true;
        }
        return !goal.getNextSweepAt().isAfter(now);
    }

    private UUID generateAutoSweepReference(AccountGoal goal, AccountGoalCadence cadence, Instant now) {
        String key = goal.getId() + ":" + cadence.periodKey(now);
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private AccountGoal loadGoal(UUID accountId, UUID goalId) {
        return goalRepository.findByIdAndAccountId(goalId, accountId)
                .orElseThrow(() -> new AccountGoalNotFoundException(goalId));
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

    private void validateGoalAmount(BigDecimal target) {
        if (target.compareTo(goalProperties.getMinGoalAmount()) < 0) {
            throw new AccountGoalValidationException("Target amount must be at least " + goalProperties.getMinGoalAmount());
        }
        if (target.compareTo(goalProperties.getMaxGoalAmount()) > 0) {
            throw new AccountGoalValidationException("Target amount exceeds maximum of " + goalProperties.getMaxGoalAmount());
        }
    }

    private void ensureGoalIsActive(AccountGoal goal) {
        if (goal.getStatus() != AccountGoalStatus.ACTIVE) {
            throw new AccountGoalValidationException("Goal must be ACTIVE to accept contributions");
        }
    }

    private BigDecimal determineAutoSweepAmount(BigDecimal requested) {
        if (requested == null) {
            return goalProperties.getAutoSweep().getDefaultContributionAmount();
        }
        return normalize(requested);
    }

    private AccountGoalCadence resolveCadence(AccountGoalCadence cadence) {
        return cadence != null ? cadence : goalProperties.getDefaultCadence();
    }

    private BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}

