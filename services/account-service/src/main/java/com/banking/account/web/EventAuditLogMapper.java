package com.banking.account.web;

import com.banking.account.audit.EventAuditLog;
import com.banking.account.web.dto.EventAuditLogResponse;

public final class EventAuditLogMapper {

    private EventAuditLogMapper() {
    }

    public static EventAuditLogResponse toResponse(EventAuditLog log) {
        return new EventAuditLogResponse(
                log.getId(),
                log.getDirection(),
                log.getStatus(),
                log.getTopic(),
                log.getEventType(),
                log.getEventKey(),
                log.getRecordPartition(),
                log.getRecordOffset(),
                log.getErrorMessage(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}


