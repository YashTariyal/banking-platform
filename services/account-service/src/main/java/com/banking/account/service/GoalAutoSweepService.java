package com.banking.account.service;

import com.banking.account.config.AccountGoalProperties;
import com.banking.account.domain.AccountGoal;
import com.banking.account.domain.AccountGoalStatus;
import com.banking.account.repository.AccountGoalRepository;
import java.time.Instant;
import java.time.Clock;
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
    private final Clock clock;

    public GoalAutoSweepService(
            AccountGoalRepository goalRepository,
            AccountGoalService accountGoalService,
            AccountGoalProperties goalProperties,
            Clock clock
    ) {
        this.goalRepository = goalRepository;
        this.accountGoalService = accountGoalService;
        this.goalProperties = goalProperties;
        this.clock = clock;
    }

    @Scheduled(cron = "${account.goals.auto-sweep.cron:0 15 1 * * ?}")
    public void runAutoSweep() {
        if (!goalProperties.getAutoSweep().isEnabled()) {
            log.debug("Goal auto-sweep disabled. Skipping run.");
            return;
        }

        int batchSize = Math.max(1, goalProperties.getAutoSweep().getBatchSize());
        int maxBatches = Math.max(1, goalProperties.getAutoSweep().getMaxBatchesPerRun());
        Pageable pageable = PageRequest.of(0, batchSize);
        Instant now = clock.instant();
        int processed = 0;

        for (int i = 0; i < maxBatches; i++) {
            Page<AccountGoal> dueGoals = goalRepository.findDueGoals(AccountGoalStatus.ACTIVE, now, pageable);
            if (dueGoals.isEmpty()) {
                break;
            }
            int processedThisBatch = 0;
            for (AccountGoal goal : dueGoals) {
                try {
                    accountGoalService.processAutoSweep(goal.getId(), now);
                    processedThisBatch++;
                } catch (Exception ex) {
                    log.warn("Auto-sweep failed for goal {}", goal.getId(), ex);
                }
            }
            processed += processedThisBatch;
            if (dueGoals.getNumberOfElements() < batchSize || processedThisBatch == 0) {
                break;
            }
        }

        if (processed == 0) {
            log.trace("No goals eligible for auto-sweep.");
        } else {
            log.info("Processed {} goal auto-sweeps", processed);
        }
    }
}

