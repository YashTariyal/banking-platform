package com.banking.card.audit;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Aspect
@Component
public class EventMonitoringAspect {

    private final EventAuditService eventAuditService;

    public EventMonitoringAspect(EventAuditService eventAuditService) {
        this.eventAuditService = eventAuditService;
    }

    @Around("execution(* org.springframework.kafka.core.KafkaTemplate.send(..))")
    public Object aroundKafkaSend(ProceedingJoinPoint joinPoint) throws Throwable {
        EventMetadata metadata = extractProducerMetadata(joinPoint.getArgs());
        if (!metadata.isValid()) {
            return joinPoint.proceed();
        }

        var auditLog = eventAuditService.recordPublishAttempt(
                metadata.topic(),
                metadata.key(),
                metadata.payload(),
                metadata.eventType()
        );

        try {
            Object result = joinPoint.proceed();
            attachCallbacks(result, auditLog.getId());
            return result;
        } catch (Throwable ex) {
            eventAuditService.markPublishFailure(auditLog.getId(), ex);
            throw ex;
        }
    }

    @Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    public Object aroundKafkaListener(ProceedingJoinPoint joinPoint) throws Throwable {
        ConsumerEventMetadata metadata = extractConsumerMetadata(joinPoint.getArgs());
        if (!metadata.valid()) {
            return joinPoint.proceed();
        }

        try {
            Object result = joinPoint.proceed();
            eventAuditService.recordConsumeSuccess(
                    metadata.topic(),
                    metadata.key(),
                    metadata.payload(),
                    metadata.eventType(),
                    metadata.partition(),
                    metadata.offset()
            );
            return result;
        } catch (Throwable ex) {
            eventAuditService.recordConsumeFailure(
                    metadata.topic(),
                    metadata.key(),
                    metadata.payload(),
                    metadata.eventType(),
                    metadata.partition(),
                    metadata.offset(),
                    ex
            );
            throw ex;
        }
    }

    private void attachCallbacks(Object kafkaSendResult, java.util.UUID auditId) {
        if (kafkaSendResult instanceof CompletionStage<?> completionStage) {
            completionStage.whenComplete((maybeResult, throwable) -> {
                if (throwable != null) {
                    eventAuditService.markPublishFailure(auditId, unwrapThrowable(throwable));
                    return;
                }
                RecordMetadata metadata = extractRecordMetadata(maybeResult);
                eventAuditService.markPublishSuccess(auditId, metadata);
            });
        } else if (kafkaSendResult instanceof ListenableFuture<?> listenableFuture) {
            listenableFuture.addCallback(new ListenableFutureCallback<Object>() {
                @Override
                public void onFailure(Throwable ex) {
                    eventAuditService.markPublishFailure(auditId, ex);
                }

                @Override
                public void onSuccess(Object result) {
                    RecordMetadata metadata = extractRecordMetadata(result);
                    eventAuditService.markPublishSuccess(auditId, metadata);
                }
            });
        } else {
            eventAuditService.markPublishSuccess(auditId, null);
        }
    }

    private Throwable unwrapThrowable(Throwable throwable) {
        if (throwable instanceof ExecutionException executionException && executionException.getCause() != null) {
            return executionException.getCause();
        }
        return throwable;
    }

    private RecordMetadata extractRecordMetadata(Object result) {
        if (result instanceof org.springframework.kafka.support.SendResult<?, ?> sendResult) {
            return sendResult.getRecordMetadata();
        }
        return null;
    }

    private EventMetadata extractProducerMetadata(Object[] args) {
        if (args == null || args.length == 0) {
            return EventMetadata.invalid();
        }

        Object firstArg = args[0];
        if (firstArg instanceof ProducerRecord<?, ?> producerRecord) {
            return new EventMetadata(
                    producerRecord.topic(),
                    producerRecord.key() != null ? producerRecord.key().toString() : null,
                    producerRecord.value(),
                    resolveEventType(producerRecord.value())
            );
        }

        if (firstArg instanceof String topic) {
            Object key = null;
            Object payload = null;
            if (args.length == 2) {
                payload = args[1];
            } else if (args.length >= 3) {
                key = args[1];
                payload = args[2];
            }

            return new EventMetadata(
                    topic,
                    key != null ? key.toString() : null,
                    payload,
                    resolveEventType(payload)
            );
        }

        return EventMetadata.invalid();
    }

    private ConsumerEventMetadata extractConsumerMetadata(Object[] args) {
        if (args == null || args.length == 0) {
            return ConsumerEventMetadata.invalid();
        }

        for (Object arg : args) {
            if (arg instanceof ConsumerRecord<?, ?> record) {
                String topic = record.topic();
                String key = record.key() != null ? record.key().toString() : null;
                Object payload = record.value();
                String eventType = resolveEventType(payload);
                return new ConsumerEventMetadata(
                        topic,
                        key,
                        payload,
                        eventType,
                        record.partition(),
                        record.offset(),
                        true
                );
            }
        }

        for (Object arg : args) {
            if (arg instanceof Iterable<?> iterable) {
                ConsumerRecord<?, ?> firstRecord = null;
                for (Object item : iterable) {
                    if (item instanceof ConsumerRecord<?, ?> candidate) {
                        firstRecord = candidate;
                        break;
                    }
                }
                if (firstRecord != null) {
                    String topic = firstRecord.topic();
                    String key = firstRecord.key() != null ? firstRecord.key().toString() : null;
                    Object payload = firstRecord.value();
                    String eventType = resolveEventType(payload);
                    return new ConsumerEventMetadata(
                            topic,
                            key,
                            payload,
                            eventType,
                            firstRecord.partition(),
                            firstRecord.offset(),
                            true
                    );
                }
            }
        }

        return ConsumerEventMetadata.invalid();
    }

    private String resolveEventType(Object payload) {
        return payload != null ? payload.getClass().getSimpleName() : null;
    }

    private record EventMetadata(String topic, String key, Object payload, String eventType,
                                 boolean valid) {
        EventMetadata(String topic, String key, Object payload, String eventType) {
            this(topic, key, payload, eventType, topic != null);
        }

        static EventMetadata invalid() {
            return new EventMetadata(null, null, null, null, false);
        }

        boolean isValid() {
            return valid;
        }
    }

    private record ConsumerEventMetadata(
            String topic,
            String key,
            Object payload,
            String eventType,
            Integer partition,
            Long offset,
            boolean valid
    ) {
        static ConsumerEventMetadata invalid() {
            return new ConsumerEventMetadata(null, null, null, null, null, null, false);
        }

        public boolean valid() {
            return valid;
        }
    }
}


