package com.banking.transaction.audit;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventAuditLogRepository extends JpaRepository<EventAuditLog, UUID> {
}


