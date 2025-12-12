package com.banking.kyc.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventAuditServiceTest {

    @Mock
    private EventAuditLogRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EventAuditMetrics metrics;

    @InjectMocks
    private EventAuditService eventAuditService;

    @BeforeEach
    void setUp() {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"data\"}");
    }

    @Test
    void recordPublishAttempt_createsPendingLog() {
        String topic = "kyc.topic";
        String key = "key-123";
        Object payload = new Object();
        String eventType = "KYC_CASE_CREATED";

        EventAuditLog savedLog = new EventAuditLog();
        savedLog.setId(UUID.randomUUID());
        when(repository.save(any(EventAuditLog.class))).thenReturn(savedLog);

        EventAuditLog result = eventAuditService.recordPublishAttempt(topic, key, payload, eventType);

        assertNotNull(result);
        verify(repository).save(any(EventAuditLog.class));
    }

    @Test
    void markPublishSuccess_updatesLogStatus() {
        UUID logId = UUID.randomUUID();
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("kyc.topic", 0),
                0L,
                0L,
                0L,
                0L,
                0,
                0
        );

        EventAuditLog existingLog = new EventAuditLog();
        existingLog.setId(logId);
        when(repository.findById(logId)).thenReturn(java.util.Optional.of(existingLog));
        when(repository.save(any(EventAuditLog.class))).thenReturn(existingLog);

        eventAuditService.markPublishSuccess(logId, metadata);

        verify(repository).findById(logId);
        verify(repository).save(any(EventAuditLog.class));
        verify(metrics).incrementPublishSuccess();
    }

    @Test
    void markPublishFailure_updatesLogStatus() {
        UUID logId = UUID.randomUUID();
        RuntimeException exception = new RuntimeException("Test error");

        EventAuditLog existingLog = new EventAuditLog();
        existingLog.setId(logId);
        when(repository.findById(logId)).thenReturn(java.util.Optional.of(existingLog));
        when(repository.save(any(EventAuditLog.class))).thenReturn(existingLog);

        eventAuditService.markPublishFailure(logId, exception);

        verify(repository).findById(logId);
        verify(repository).save(any(EventAuditLog.class));
        verify(metrics).incrementPublishFailure();
    }

    @Test
    void recordConsumeSuccess_createsSuccessLog() {
        String topic = "kyc.topic";
        String key = "key-123";
        Object payload = new Object();
        String eventType = "KYC_CASE_CREATED";
        Integer partition = 0;
        Long offset = 100L;

        EventAuditLog savedLog = new EventAuditLog();
        when(repository.save(any(EventAuditLog.class))).thenReturn(savedLog);

        eventAuditService.recordConsumeSuccess(topic, key, payload, eventType, partition, offset);

        verify(repository).save(any(EventAuditLog.class));
        verify(metrics).incrementConsumeSuccess();
    }

    @Test
    void recordConsumeFailure_createsFailureLog() {
        String topic = "kyc.topic";
        String key = "key-123";
        Object payload = new Object();
        String eventType = "KYC_CASE_CREATED";
        Integer partition = 0;
        Long offset = 100L;
        RuntimeException exception = new RuntimeException("Consume error");

        EventAuditLog savedLog = new EventAuditLog();
        when(repository.save(any(EventAuditLog.class))).thenReturn(savedLog);

        eventAuditService.recordConsumeFailure(topic, key, payload, eventType, partition, offset, exception);

        verify(repository).save(any(EventAuditLog.class));
        verify(metrics).incrementConsumeFailure();
    }
}
