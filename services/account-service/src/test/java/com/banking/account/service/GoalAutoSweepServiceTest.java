package com.banking.account.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.account.config.AccountGoalProperties;
import com.banking.account.domain.AccountGoal;
import com.banking.account.domain.AccountGoalStatus;
import com.banking.account.repository.AccountGoalRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class GoalAutoSweepServiceTest {

    @Mock
    private AccountGoalRepository goalRepository;
    @Mock
    private AccountGoalService accountGoalService;

    private GoalAutoSweepService goalAutoSweepService;
    private AccountGoalProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AccountGoalProperties();
        goalAutoSweepService = new GoalAutoSweepService(goalRepository, accountGoalService, properties);
    }

    @Test
    void runAutoSweepInvokesServiceForCandidates() {
        AccountGoal goal = new AccountGoal();
        goal.setId(java.util.UUID.randomUUID());
        goal.setStatus(AccountGoalStatus.ACTIVE);
        goal.setAutoSweepEnabled(true);
        Page<AccountGoal> page = new PageImpl<>(List.of(goal));
        when(goalRepository.findByAutoSweepEnabledTrueAndStatus(any(), any(Pageable.class))).thenReturn(page);

        goalAutoSweepService.runAutoSweep();

        verify(accountGoalService, times(1)).processAutoSweep(any(), any());
    }
}

