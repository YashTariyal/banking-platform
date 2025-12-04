package com.banking.card.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventAuditLogRepository extends JpaRepository<EventAuditLog, UUID> {

    Page<EventAuditLog> findByStatus(EventStatus status, Pageable pageable);

    Page<EventAuditLog> findByDirection(EventDirection direction, Pageable pageable);

    Page<EventAuditLog> findByStatusAndDirection(EventStatus status, EventDirection direction, Pageable pageable);

    Page<EventAuditLog> findByTopicContainingIgnoreCase(String topic, Pageable pageable);
}


