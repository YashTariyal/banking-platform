package com.banking.payment.audit;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventAuditLogRepository extends JpaRepository<EventAuditLog, UUID> {
}


