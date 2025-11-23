package com.banking.account.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "account.limits")
public class AccountLimitsProperties {

    private BigDecimal minBalance = BigDecimal.ZERO;
    private BigDecimal maxBalance = new BigDecimal("999999999.99");
    private BigDecimal maxTransactionAmount = new BigDecimal("1000000.00");
    private int maxDailyTransactions = 100;
    private BigDecimal maxDailyTransactionAmount = new BigDecimal("50000.00");

    public BigDecimal getMinBalance() {
        return minBalance;
    }

    public void setMinBalance(BigDecimal minBalance) {
        this.minBalance = minBalance;
    }

    public BigDecimal getMaxBalance() {
        return maxBalance;
    }

    public void setMaxBalance(BigDecimal maxBalance) {
        this.maxBalance = maxBalance;
    }

    public BigDecimal getMaxTransactionAmount() {
        return maxTransactionAmount;
    }

    public void setMaxTransactionAmount(BigDecimal maxTransactionAmount) {
        this.maxTransactionAmount = maxTransactionAmount;
    }

    public int getMaxDailyTransactions() {
        return maxDailyTransactions;
    }

    public void setMaxDailyTransactions(int maxDailyTransactions) {
        this.maxDailyTransactions = maxDailyTransactions;
    }

    public BigDecimal getMaxDailyTransactionAmount() {
        return maxDailyTransactionAmount;
    }

    public void setMaxDailyTransactionAmount(BigDecimal maxDailyTransactionAmount) {
        this.maxDailyTransactionAmount = maxDailyTransactionAmount;
    }
}

