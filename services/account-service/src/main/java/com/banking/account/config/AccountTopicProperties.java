package com.banking.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "account.topics")
public class AccountTopicProperties {

    /**
     * Topic used when a new account is created.
     */
    private String accountCreated;

    /**
     * Topic used when an existing account is updated.
     */
    private String accountUpdated;

    public String getAccountCreated() {
        return accountCreated;
    }

    public void setAccountCreated(String accountCreated) {
        this.accountCreated = accountCreated;
    }

    public String getAccountUpdated() {
        return accountUpdated;
    }

    public void setAccountUpdated(String accountUpdated) {
        this.accountUpdated = accountUpdated;
    }
}

