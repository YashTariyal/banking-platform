package com.banking.account.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "account.transactions.retention")
public class TransactionRetentionProperties {

    private Duration archiveAfter = Duration.ofDays(365); // Archive after 1 year
    private Duration deleteAfter = Duration.ofDays(2555); // Delete after 7 years (compliance)
    private boolean archiveEnabled = true;
    private boolean deleteEnabled = false; // Disabled by default for safety

    public Duration getArchiveAfter() {
        return archiveAfter;
    }

    public void setArchiveAfter(Duration archiveAfter) {
        this.archiveAfter = archiveAfter;
    }

    public Duration getDeleteAfter() {
        return deleteAfter;
    }

    public void setDeleteAfter(Duration deleteAfter) {
        this.deleteAfter = deleteAfter;
    }

    public boolean isArchiveEnabled() {
        return archiveEnabled;
    }

    public void setArchiveEnabled(boolean archiveEnabled) {
        this.archiveEnabled = archiveEnabled;
    }

    public boolean isDeleteEnabled() {
        return deleteEnabled;
    }

    public void setDeleteEnabled(boolean deleteEnabled) {
        this.deleteEnabled = deleteEnabled;
    }
}

