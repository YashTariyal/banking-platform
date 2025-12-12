package com.banking.support.audit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventMonitoringAspectTest {

    @Mock
    private EventAuditService eventAuditService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectMocks
    private EventMonitoringAspect aspect;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void aroundKafkaListener_recordsConsumeSuccess() throws Throwable {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("support.topic", 1, 5L, "key-1", "payload");

        when(joinPoint.getArgs()).thenReturn(new Object[]{record});
        when(joinPoint.proceed()).thenReturn(null);

        aspect.aroundKafkaListener(joinPoint);

        verify(eventAuditService).recordConsumeSuccess(
                "support.topic",
                "key-1",
                record.value(),
                "String",
                1,
                5L
        );
    }

    @Test
    void aroundKafkaListener_recordsConsumeFailureOnException() throws Throwable {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("support.topic", 2, 10L, "key-2", "payload-2");

        when(joinPoint.getArgs()).thenReturn(new Object[]{record});
        doThrow(new RuntimeException("listener failure")).when(joinPoint).proceed();

        try {
            aspect.aroundKafkaListener(joinPoint);
        } catch (RuntimeException ignored) {
            // expected
        }

        verify(eventAuditService).recordConsumeFailure(
                eq("support.topic"),
                eq("key-2"),
                eq(record.value()),
                eq("String"),
                eq(2),
                eq(10L),
                any(RuntimeException.class)
        );
    }
}
