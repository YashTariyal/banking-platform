package com.banking.account.config;

import com.banking.account.domain.AccountGoalCadence;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "account.goals")
public class AccountGoalProperties {

    private AccountGoalCadence defaultCadence = AccountGoalCadence.MONTHLY;
    private BigDecimal minGoalAmount = new BigDecimal("10.00");
    private BigDecimal maxGoalAmount = new BigDecimal("1000000.00");
    private AutoSweep autoSweep = new AutoSweep();

    public AccountGoalCadence getDefaultCadence() {
        return defaultCadence;
    }

    public void setDefaultCadence(AccountGoalCadence defaultCadence) {
        this.defaultCadence = defaultCadence;
    }

    public BigDecimal getMinGoalAmount() {
        return minGoalAmount;
    }

    public void setMinGoalAmount(BigDecimal minGoalAmount) {
        this.minGoalAmount = minGoalAmount;
    }

    public BigDecimal getMaxGoalAmount() {
        return maxGoalAmount;
    }

    public void setMaxGoalAmount(BigDecimal maxGoalAmount) {
        this.maxGoalAmount = maxGoalAmount;
    }

    public AutoSweep getAutoSweep() {
        return autoSweep;
    }

    public void setAutoSweep(AutoSweep autoSweep) {
        this.autoSweep = autoSweep;
    }

    public static class AutoSweep {

        private boolean enabled = true;
        private String cron = "0 15 1 * * ?";
        private BigDecimal minBalanceBuffer = new BigDecimal("50.00");
        private BigDecimal defaultContributionAmount = new BigDecimal("50.00");
        private BigDecimal minContributionAmount = new BigDecimal("10.00");
        private BigDecimal maxContributionAmount = new BigDecimal("1000.00");
        private int batchSize = 100;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public BigDecimal getMinBalanceBuffer() {
            return minBalanceBuffer;
        }

        public void setMinBalanceBuffer(BigDecimal minBalanceBuffer) {
            this.minBalanceBuffer = minBalanceBuffer;
        }

        public BigDecimal getDefaultContributionAmount() {
            return defaultContributionAmount;
        }

        public void setDefaultContributionAmount(BigDecimal defaultContributionAmount) {
            this.defaultContributionAmount = defaultContributionAmount;
        }

        public BigDecimal getMinContributionAmount() {
            return minContributionAmount;
        }

        public void setMinContributionAmount(BigDecimal minContributionAmount) {
            this.minContributionAmount = minContributionAmount;
        }

        public BigDecimal getMaxContributionAmount() {
            return maxContributionAmount;
        }

        public void setMaxContributionAmount(BigDecimal maxContributionAmount) {
            this.maxContributionAmount = maxContributionAmount;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }
}

