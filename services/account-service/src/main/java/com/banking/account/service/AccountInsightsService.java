package com.banking.account.service;

import com.banking.account.domain.Account;
import com.banking.account.domain.AccountGoal;
import com.banking.account.domain.AccountTransactionLog;
import com.banking.account.repository.AccountGoalRepository;
import com.banking.account.repository.AccountTransactionLogRepository;
import com.banking.account.web.dto.AccountSummaryResponse;
import com.banking.account.web.dto.AccountSummaryResponse.GoalSnapshot;
import com.banking.account.web.dto.AccountSummaryResponse.RecentTransaction;
import com.banking.account.security.TenantAccessEvaluator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountInsightsService {

    private static final int RECENT_TRANSACTION_LIMIT = 10;
    private static final int GOAL_LIMIT = 20;

    private final AccountService accountService;
    private final AccountTransactionLogRepository transactionLogRepository;
    private final AccountGoalRepository goalRepository;
    private final TenantAccessEvaluator tenantAccessEvaluator;

    public AccountInsightsService(
            AccountService accountService,
            AccountTransactionLogRepository transactionLogRepository,
            AccountGoalRepository goalRepository,
            TenantAccessEvaluator tenantAccessEvaluator
    ) {
        this.accountService = accountService;
        this.transactionLogRepository = transactionLogRepository;
        this.goalRepository = goalRepository;
        this.tenantAccessEvaluator = tenantAccessEvaluator;
    }

    public AccountSummaryResponse getAccountSummary(UUID accountId) {
        // Load account and enforce tenant checks
        Account account = accountService.loadAccount(accountId);
        tenantAccessEvaluator.assertCanAccessAccount(account);

        List<AccountTransactionLog> recentLogs = transactionLogRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(0, RECENT_TRANSACTION_LIMIT))
                .getContent();

        List<AccountGoal> goals = goalRepository
                .findByAccountId(accountId, PageRequest.of(0, GOAL_LIMIT))
                .getContent();

        List<RecentTransaction> txDtos = recentLogs.stream()
                .map(log -> new RecentTransaction(
                        log.getReferenceId(),
                        log.getType().name(),
                        log.getAmount(),
                        log.getResultingBalance(),
                        log.getCreatedAt()
                ))
                .collect(Collectors.toList());

        List<GoalSnapshot> goalDtos = goals.stream()
                .map(goal -> new GoalSnapshot(
                        goal.getId(),
                        goal.getName(),
                        goal.getCurrentAmount(),
                        goal.getTargetAmount(),
                        goal.getStatus().name(),
                        goal.getDueDate()
                ))
                .collect(Collectors.toList());

        return new AccountSummaryResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency(),
                recentLogs.size(),
                txDtos,
                goalDtos
        );
    }
}


