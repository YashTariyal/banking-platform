package com.banking.account.messaging;

import com.banking.account.domain.AccountTransactionType;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logger for transaction audit details.
 * Logs all transaction operations with masked PII for audit compliance.
 */
@Component
public class TransactionAuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    public void logTransaction(UUID accountId, UUID referenceId, AccountTransactionType type, 
                              BigDecimal amount, BigDecimal resultingBalance, String description) {
        auditLog.info("Transaction processed - accountId: {}*** referenceId: {} type: {} amount: {} resultingBalance: {} description: {}",
                maskId(accountId),
                maskId(referenceId),
                type,
                amount,
                resultingBalance,
                description != null ? description : "N/A");
    }

    private String maskId(UUID id) {
        if (id == null) {
            return "***";
        }
        String idStr = id.toString();
        return idStr.length() > 4 ? "***" + idStr.substring(idStr.length() - 4) : "***";
    }
}

