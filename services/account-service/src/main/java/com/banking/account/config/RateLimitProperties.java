package com.banking.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "account.rate-limit")
public class RateLimitProperties {

    private int defaultRequestsPerMinute = 60;
    private int defaultRequestsPerHour = 1000;
    private int createAccountRequestsPerMinute = 10;
    private int transactionRequestsPerMinute = 100;
    private int readRequestsPerMinute = 200;
    private boolean enabled = true;

    public int getDefaultRequestsPerMinute() {
        return defaultRequestsPerMinute;
    }

    public void setDefaultRequestsPerMinute(int defaultRequestsPerMinute) {
        this.defaultRequestsPerMinute = defaultRequestsPerMinute;
    }

    public int getDefaultRequestsPerHour() {
        return defaultRequestsPerHour;
    }

    public void setDefaultRequestsPerHour(int defaultRequestsPerHour) {
        this.defaultRequestsPerHour = defaultRequestsPerHour;
    }

    public int getCreateAccountRequestsPerMinute() {
        return createAccountRequestsPerMinute;
    }

    public void setCreateAccountRequestsPerMinute(int createAccountRequestsPerMinute) {
        this.createAccountRequestsPerMinute = createAccountRequestsPerMinute;
    }

    public int getTransactionRequestsPerMinute() {
        return transactionRequestsPerMinute;
    }

    public void setTransactionRequestsPerMinute(int transactionRequestsPerMinute) {
        this.transactionRequestsPerMinute = transactionRequestsPerMinute;
    }

    public int getReadRequestsPerMinute() {
        return readRequestsPerMinute;
    }

    public void setReadRequestsPerMinute(int readRequestsPerMinute) {
        this.readRequestsPerMinute = readRequestsPerMinute;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

