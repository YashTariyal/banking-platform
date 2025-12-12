package com.banking.loan.audit;

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
    private final EventAuditMetrics metrics;

    public EventAuditService(
            EventAuditLogRepository repository,
            ObjectMapper objectMapper,
            EventAuditMetrics metrics
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
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
        metrics.incrementPublishSuccess();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublishFailure(UUID id, Throwable throwable) {
        updateLog(id, EventStatus.FAILED, throwable != null ? throwable.getMessage() : "unknown error", null);
        metrics.incrementPublishFailure();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordConsumeSuccess(
            String topic,
            String key,
            Object payload,
            String eventType,
            Integer partition,
            Long offset
    ) {
        EventAuditLog log = new EventAuditLog();
        log.setId(UUID.randomUUID());
        log.setDirection(EventDirection.CONSUME);
        log.setStatus(EventStatus.SUCCESS);
        log.setTopic(topic);
        log.setEventType(eventType);
        log.setEventKey(key);
        log.setPayload(serializePayload(payload));
        log.setRecordPartition(partition);
        log.setRecordOffset(offset);
        log.setCreatedAt(Instant.now());
        log.setUpdatedAt(log.getCreatedAt());
        repository.save(log);
        metrics.incrementConsumeSuccess();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordConsumeFailure(
            String topic,
            String key,
            Object payload,
            String eventType,
            Integer partition,
            Long offset,
            Throwable throwable
    ) {
        EventAuditLog log = new EventAuditLog();
        log.setId(UUID.randomUUID());
        log.setDirection(EventDirection.CONSUME);
        log.setStatus(EventStatus.FAILED);
        log.setTopic(topic);
        log.setEventType(eventType);
        log.setEventKey(key);
        log.setPayload(serializePayload(payload));
        log.setRecordPartition(partition);
        log.setRecordOffset(offset);
        log.setErrorMessage(throwable != null ? throwable.getMessage() : "unknown error");
        log.setCreatedAt(Instant.now());
        log.setUpdatedAt(log.getCreatedAt());
        repository.save(log);
        metrics.incrementConsumeFailure();
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
