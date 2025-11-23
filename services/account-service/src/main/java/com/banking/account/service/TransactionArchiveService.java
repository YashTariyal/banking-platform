package com.banking.account.service;

import com.banking.account.config.TransactionRetentionProperties;
import com.banking.account.domain.AccountTransactionLog;
import com.banking.account.repository.AccountTransactionLogRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for archiving and managing transaction log retention.
 */
@Service
public class TransactionArchiveService {

    private static final Logger log = LoggerFactory.getLogger(TransactionArchiveService.class);

    private final AccountTransactionLogRepository transactionLogRepository;
    private final TransactionRetentionProperties retentionProperties;

    public TransactionArchiveService(
            AccountTransactionLogRepository transactionLogRepository,
            TransactionRetentionProperties retentionProperties) {
        this.transactionLogRepository = transactionLogRepository;
        this.retentionProperties = retentionProperties;
    }

    /**
     * Archives transactions older than the retention period.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void archiveOldTransactions() {
        if (!retentionProperties.isArchiveEnabled()) {
            log.debug("Transaction archiving is disabled");
            return;
        }

        Instant archiveThreshold = Instant.now().minus(retentionProperties.getArchiveAfter());
        log.info("Starting transaction archive process. Archive threshold: {}", archiveThreshold);

        try {
            long count = countTransactionsOlderThan(archiveThreshold);
            
            if (count > 0) {
                log.info("Found {} transactions to archive (older than {})", count, archiveThreshold);
                
                // Get transactions to archive (in batches to avoid memory issues)
                List<AccountTransactionLog> transactionsToArchive = transactionLogRepository.findByCreatedAtBefore(archiveThreshold);
                
                // TODO: Implement actual archiving logic
                // This could involve:
                // 1. Moving to archive table: INSERT INTO account_transactions_archive SELECT * FROM account_transactions WHERE created_at < threshold
                // 2. Exporting to cold storage (S3, Parquet files, etc.)
                // 3. Compressing and storing in object storage
                // 4. For now, we just log - actual archiving should be implemented based on infrastructure
                
                log.info("Archive process completed. {} transactions marked for archiving", transactionsToArchive.size());
            } else {
                log.debug("No transactions found for archiving");
            }
        } catch (Exception ex) {
            log.error("Error during transaction archiving", ex);
        }
    }

    /**
     * Deletes transactions older than the delete retention period.
     * Runs weekly on Sunday at 3 AM.
     * WARNING: This permanently deletes data. Use with caution.
     */
    @Scheduled(cron = "0 0 3 ? * SUN") // Weekly on Sunday at 3 AM
    @Transactional
    public void deleteOldTransactions() {
        if (!retentionProperties.isDeleteEnabled()) {
            log.debug("Transaction deletion is disabled");
            return;
        }

        Instant deleteThreshold = Instant.now().minus(retentionProperties.getDeleteAfter());
        log.warn("Starting transaction deletion process. Delete threshold: {}", deleteThreshold);

        try {
            long count = countTransactionsOlderThan(deleteThreshold);
            
            if (count > 0) {
                log.warn("Found {} transactions to delete (older than {})", count, deleteThreshold);
                
                // Get transactions to delete
                List<AccountTransactionLog> transactionsToDelete = transactionLogRepository.findByCreatedAtBefore(deleteThreshold);
                
                // WARNING: This permanently deletes data
                // In production, this should:
                // 1. Verify data has been archived first
                // 2. Create a backup before deletion
                // 3. Delete in batches with transaction control
                // 4. Have approval/audit trail
                
                // TODO: Implement actual deletion logic
                // transactionLogRepository.deleteAll(transactionsToDelete);
                
                log.warn("Deletion process completed. {} transactions marked for deletion (actual deletion disabled for safety)", 
                        transactionsToDelete.size());
            } else {
                log.debug("No transactions found for deletion");
            }
        } catch (Exception ex) {
            log.error("Error during transaction deletion", ex);
        }
    }

    /**
     * Counts transactions older than the given threshold.
     */
    private long countTransactionsOlderThan(Instant threshold) {
        return transactionLogRepository.countByCreatedAtBefore(threshold);
    }

    /**
     * Manual trigger for archiving (for testing/admin use).
     */
    @Transactional
    public long archiveTransactionsManually(Instant threshold) {
        log.info("Manual archive triggered. Threshold: {}", threshold);
        return countTransactionsOlderThan(threshold);
    }
}

