package com.banking.risk.audit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class EventAuditMetrics {

    private final Counter publishSuccess;
    private final Counter publishFailure;
    private final Counter consumeSuccess;
    private final Counter consumeFailure;

    public EventAuditMetrics(MeterRegistry meterRegistry) {
        this.publishSuccess = Counter.builder("events.published")
                .tag("service", "risk-service")
                .tag("status", "success")
                .description("Successfully published Kafka events")
                .register(meterRegistry);

        this.publishFailure = Counter.builder("events.published")
                .tag("service", "risk-service")
                .tag("status", "failure")
                .description("Failed Kafka publish attempts")
                .register(meterRegistry);

        this.consumeSuccess = Counter.builder("events.consumed")
                .tag("service", "risk-service")
                .tag("status", "success")
                .description("Successfully consumed Kafka events")
                .register(meterRegistry);

        this.consumeFailure = Counter.builder("events.consumed")
                .tag("service", "risk-service")
                .tag("status", "failure")
                .description("Failed Kafka event consumptions")
                .register(meterRegistry);
    }

    public void incrementPublishSuccess() {
        publishSuccess.increment();
    }

    public void incrementPublishFailure() {
        publishFailure.increment();
    }

    public void incrementConsumeSuccess() {
        consumeSuccess.increment();
    }

    public void incrementConsumeFailure() {
        consumeFailure.increment();
    }
}
