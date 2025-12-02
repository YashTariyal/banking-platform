package com.banking.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.account.config.AccountGoalProperties;
import com.banking.account.domain.Account;
import com.banking.account.domain.AccountGoal;
import com.banking.account.domain.AccountGoalStatus;
import com.banking.account.metrics.AccountMetrics;
import com.banking.account.repository.AccountGoalContributionRepository;
import com.banking.account.repository.AccountGoalRepository;
import com.banking.account.repository.AccountRepository;
import com.banking.account.web.dto.AccountGoalResponse;
import com.banking.account.web.dto.CreateAccountGoalRequest;
import com.banking.account.web.dto.GoalContributionRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountGoalServiceTest {

    @Mock
    private AccountGoalRepository goalRepository;
    @Mock
    private AccountGoalContributionRepository contributionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private AccountMetrics accountMetrics;
    @Mock
    private com.banking.account.security.TenantAccessEvaluator tenantAccessEvaluator;

    private AccountGoalService accountGoalService;

    @BeforeEach
    void setUp() {
        AccountGoalProperties properties = new AccountGoalProperties();
        accountGoalService = new AccountGoalService(
                goalRepository,
                contributionRepository,
                accountRepository,
                accountService,
                properties,
                accountMetrics,
                tenantAccessEvaluator
        );
    }

    @Test
    void createGoalPersistsAndReturnsResponse() {
        UUID accountId = UUID.randomUUID();
        Account account = buildAccount(accountId, BigDecimal.valueOf(500));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(goalRepository.save(any(AccountGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateAccountGoalRequest request = new CreateAccountGoalRequest(
                "Vacation",
                "Trip fund",
                new BigDecimal("1000"),
                null,
                true,
                new BigDecimal("100"),
                null
        );

        AccountGoalResponse response = accountGoalService.createGoal(accountId, request);

        assertThat(response.name()).isEqualTo("Vacation");
        assertThat(response.autoSweepEnabled()).isTrue();
        verify(accountMetrics).incrementGoalCreated();
    }

    @Test
    void contributeToGoalDebitsAccount() {
        UUID accountId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        AccountGoal goal = buildGoal(goalId, accountId, new BigDecimal("500"), new BigDecimal("100"));
        when(goalRepository.findByIdAndAccountId(goalId, accountId)).thenReturn(Optional.of(goal));
        when(goalRepository.save(goal)).thenReturn(goal);

        GoalContributionRequest request = new GoalContributionRequest(new BigDecimal("50"), null, "Top up");
        accountGoalService.contributeToGoal(accountId, goalId, request);

        ArgumentCaptor<com.banking.account.web.dto.AccountTransactionRequest> txCaptor =
                ArgumentCaptor.forClass(com.banking.account.web.dto.AccountTransactionRequest.class);

        verify(accountService).applyTransaction(eq(accountId), txCaptor.capture());
        assertThat(txCaptor.getValue().amount()).isEqualByComparingTo("50.00");
        verify(contributionRepository).save(any());
    }

    @Test
    void processAutoSweepSkipsWhenInsufficientBuffer() {
        UUID goalId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        AccountGoal goal = buildGoal(goalId, accountId, new BigDecimal("500"), new BigDecimal("100"));
        goal.setAutoSweepEnabled(true);
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
        Account lowBalanceAccount = buildAccount(accountId, new BigDecimal("20"));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(lowBalanceAccount));

        accountGoalService.processAutoSweep(goalId, Instant.now());

        verify(accountService, never()).applyTransaction(any(), any());
    }

    private Account buildAccount(UUID id, BigDecimal balance) {
        Account account = new Account();
        account.setId(id);
        account.setBalance(balance);
        account.setCurrency("USD");
        account.setStatus(com.banking.account.domain.AccountStatus.ACTIVE);
        account.setAccountNumber("ACC-" + id.toString().substring(0, 8));
        account.setCustomerId(UUID.randomUUID());
        account.setOpenedAt(Instant.now());
        account.setUpdatedAt(Instant.now());
        return account;
    }

    private AccountGoal buildGoal(UUID goalId, UUID accountId, BigDecimal target, BigDecimal current) {
        AccountGoal goal = new AccountGoal();
        goal.setId(goalId);
        goal.setAccountId(accountId);
        goal.setName("Goal");
        goal.setTargetAmount(target);
        goal.setCurrentAmount(current);
        goal.setStatus(AccountGoalStatus.ACTIVE);
        goal.setAutoSweepEnabled(false);
        goal.setAutoSweepCadence(com.banking.account.domain.AccountGoalCadence.MONTHLY);
        return goal;
    }
}

