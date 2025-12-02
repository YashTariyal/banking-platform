package com.banking.account.service;

import com.banking.account.domain.Account;
import com.banking.account.domain.AccountGoal;
import com.banking.account.domain.AccountTransactionLog;
import com.banking.account.repository.AccountGoalRepository;
import com.banking.account.repository.AccountTransactionLogRepository;
import com.banking.account.web.dto.AccountSummaryResponse;
import com.banking.account.web.dto.AccountSummaryResponse.GoalSnapshot;
import com.banking.account.web.dto.AccountSummaryResponse.RecentTransaction;
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

    public AccountInsightsService(
            AccountService accountService,
            AccountTransactionLogRepository transactionLogRepository,
            AccountGoalRepository goalRepository
    ) {
        this.accountService = accountService;
        this.transactionLogRepository = transactionLogRepository;
        this.goalRepository = goalRepository;
    }

    public AccountSummaryResponse getAccountSummary(UUID accountId) {
        // Reuse existing tenant and security checks by going through AccountService
        Account account = accountService.loadSecuredAccountInternal(accountId);

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
                        log.getDescription(),
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


