package com.banking.payment.messaging;

import com.banking.payment.domain.Payment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);
    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PaymentEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void publishPaymentInitiated(Payment payment) {
        publishEvent(createEvent("PAYMENT_INITIATED", payment));
    }

    public void publishPaymentProcessing(Payment payment) {
        publishEvent(createEvent("PAYMENT_PROCESSING", payment));
    }

    public void publishPaymentCompleted(Payment payment) {
        publishEvent(createEvent("PAYMENT_COMPLETED", payment));
    }

    public void publishPaymentFailed(Payment payment) {
        publishEvent(createEvent("PAYMENT_FAILED", payment));
    }

    public void publishPaymentCancelled(Payment payment) {
        publishEvent(createEvent("PAYMENT_CANCELLED", payment));
    }

    private PaymentEvent createEvent(String eventType, Payment payment) {
        return new PaymentEvent(
                eventType,
                payment.getId(),
                payment.getReferenceId(),
                payment.getRail(),
                payment.getDirection(),
                payment.getStatus(),
                payment.getFromAccountId(),
                payment.getToAccountId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getDescription(),
                payment.getFailureReason(),
                Instant.now(clock)
        );
    }

    private void publishEvent(PaymentEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, event.paymentId().toString(), payload);
            log.debug("Published payment event: {} for payment: {}", event.eventType(), event.paymentId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payment event: {}", event, e);
        }
    }
}

