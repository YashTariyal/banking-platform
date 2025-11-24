package com.banking.account.service;

import com.banking.account.config.AccountGoalProperties;
import com.banking.account.domain.AccountGoal;
import com.banking.account.domain.AccountGoalStatus;
import com.banking.account.repository.AccountGoalRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class GoalAutoSweepService {

    private static final Logger log = LoggerFactory.getLogger(GoalAutoSweepService.class);

    private final AccountGoalRepository goalRepository;
    private final AccountGoalService accountGoalService;
    private final AccountGoalProperties goalProperties;

    public GoalAutoSweepService(
            AccountGoalRepository goalRepository,
            AccountGoalService accountGoalService,
            AccountGoalProperties goalProperties
    ) {
        this.goalRepository = goalRepository;
        this.accountGoalService = accountGoalService;
        this.goalProperties = goalProperties;
    }

    @Scheduled(cron = "${account.goals.auto-sweep.cron:0 15 1 * * ?}")
    public void runAutoSweep() {
        if (!goalProperties.getAutoSweep().isEnabled()) {
            log.debug("Goal auto-sweep disabled. Skipping run.");
            return;
        }

        int batchSize = Math.max(1, goalProperties.getAutoSweep().getBatchSize());
        Pageable pageable = PageRequest.of(0, batchSize);
        Page<AccountGoal> candidates = goalRepository.findByAutoSweepEnabledTrueAndStatus(AccountGoalStatus.ACTIVE, pageable);
        if (candidates.isEmpty()) {
            log.trace("No goals eligible for auto-sweep.");
            return;
        }

        Instant now = Instant.now();
        candidates.forEach(goal -> {
            try {
                accountGoalService.processAutoSweep(goal.getId(), now);
            } catch (Exception ex) {
                log.warn("Auto-sweep failed for goal {}", goal.getId(), ex);
            }
        });
    }
}

