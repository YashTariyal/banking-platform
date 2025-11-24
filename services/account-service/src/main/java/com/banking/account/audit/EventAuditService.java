package com.banking.account.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventAuditService {

    private final EventAuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public EventAuditService(EventAuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventAuditLog recordPublishAttempt(String topic, String key, Object payload, String eventType) {
        EventAuditLog log = new EventAuditLog();
        log.setId(UUID.randomUUID());
        log.setDirection(EventDirection.PUBLISH);
        log.setStatus(EventStatus.PENDING);
        log.setTopic(topic);
        log.setEventType(eventType);
        log.setEventKey(key);
        log.setPayload(serializePayload(payload));
        return repository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublishSuccess(UUID id, RecordMetadata metadata) {
        updateLog(id, EventStatus.SUCCESS, null, metadata);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublishFailure(UUID id, Throwable throwable) {
        updateLog(id, EventStatus.FAILED, throwable != null ? throwable.getMessage() : "unknown error", null);
    }

    private void updateLog(UUID id, EventStatus status, String errorMessage, RecordMetadata metadata) {
        Optional<EventAuditLog> optional = repository.findById(id);
        if (optional.isEmpty()) {
            return;
        }

        EventAuditLog log = optional.get();
        log.setStatus(status);
        log.setErrorMessage(errorMessage);
        if (metadata != null) {
            log.setRecordPartition(metadata.partition());
            log.setRecordOffset(metadata.offset());
        }
        log.setUpdatedAt(Instant.now());
        repository.save(log);
    }

    private String serializePayload(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof String str) {
            return str;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return payload.toString();
        }
    }
}

