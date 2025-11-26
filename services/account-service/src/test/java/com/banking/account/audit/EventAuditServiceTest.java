package com.banking.account.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventAuditServiceTest {

    @Mock
    private EventAuditLogRepository repository;

    @Mock
    private EventAuditMetrics metrics;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private EventAuditService eventAuditService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        eventAuditService = new EventAuditService(repository, objectMapper, metrics);
    }

    @Test
    void recordPublishAttemptPersistsPendingLog() {
        when(repository.save(any(EventAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventAuditLog log = eventAuditService.recordPublishAttempt(
                "accounts.account-created",
                "account-key",
                new SamplePayload("foo"),
                "ACCOUNT_CREATED"
        );

        ArgumentCaptor<EventAuditLog> captor = ArgumentCaptor.forClass(EventAuditLog.class);
        verify(repository).save(captor.capture());

        EventAuditLog saved = captor.getValue();
        assertThat(saved.getDirection()).isEqualTo(EventDirection.PUBLISH);
        assertThat(saved.getStatus()).isEqualTo(EventStatus.PENDING);
        assertThat(saved.getTopic()).isEqualTo("accounts.account-created");
        assertThat(saved.getEventKey()).isEqualTo("account-key");
        assertThat(saved.getEventType()).isEqualTo("ACCOUNT_CREATED");
        assertThat(saved.getPayload()).contains("\"value\":\"foo\"");

        assertThat(log.getId()).isNotNull();
    }

    @Test
    void markPublishSuccessUpdatesMetadata() {
        UUID id = UUID.randomUUID();
        EventAuditLog log = new EventAuditLog();
        log.setId(id);
        log.setStatus(EventStatus.PENDING);
        log.setUpdatedAt(Instant.now());

        when(repository.findById(id)).thenReturn(Optional.of(log));

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("topic", 2), 10, 5, 0L, 0L, 0, 0);
        eventAuditService.markPublishSuccess(id, metadata);

        ArgumentCaptor<EventAuditLog> captor = ArgumentCaptor.forClass(EventAuditLog.class);
        verify(repository).save(captor.capture());

        EventAuditLog updated = captor.getValue();
        assertThat(updated.getStatus()).isEqualTo(EventStatus.SUCCESS);
        assertThat(updated.getRecordPartition()).isEqualTo(2);
        assertThat(updated.getRecordOffset()).isEqualTo(15);
    }

    @Test
    void markPublishFailureSkipsWhenLogMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        eventAuditService.markPublishFailure(id, new RuntimeException("boom"));

        verify(repository, never()).save(any(EventAuditLog.class));
    }

    private record SamplePayload(String value) {
    }
}

