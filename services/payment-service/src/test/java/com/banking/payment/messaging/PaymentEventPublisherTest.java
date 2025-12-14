package com.banking.payment.messaging;

import com.banking.payment.domain.Payment;
import com.banking.payment.domain.PaymentDirection;
import com.banking.payment.domain.PaymentRail;
import com.banking.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private PaymentEventPublisher publisher;
    private Payment testPayment;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        publisher = new PaymentEventPublisher(kafkaTemplate, objectMapper, fixedClock);
        
        testPayment = new Payment();
        testPayment.setId(UUID.randomUUID());
        testPayment.setReferenceId(UUID.randomUUID().toString());
        testPayment.setRail(PaymentRail.ACH);
        testPayment.setDirection(PaymentDirection.OUTBOUND);
        testPayment.setStatus(PaymentStatus.COMPLETED);
        testPayment.setFromAccountId(UUID.randomUUID());
        testPayment.setToAccountId(UUID.randomUUID());
        testPayment.setAmount(new BigDecimal("500.00"));
        testPayment.setCurrency("USD");
        testPayment.setDescription("Test payment");

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.complete(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenReturn(future);
    }

    @Test
    void publishPaymentInitiated_PublishesEvent() {
        publisher.publishPaymentInitiated(testPayment);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("payment-events"), eq(testPayment.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("PAYMENT_INITIATED");
        assertThat(payloadCaptor.getValue()).contains(testPayment.getId().toString());
    }

    @Test
    void publishPaymentProcessing_PublishesEvent() {
        publisher.publishPaymentProcessing(testPayment);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("payment-events"), eq(testPayment.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("PAYMENT_PROCESSING");
    }

    @Test
    void publishPaymentCompleted_PublishesEvent() {
        publisher.publishPaymentCompleted(testPayment);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("payment-events"), eq(testPayment.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("PAYMENT_COMPLETED");
    }

    @Test
    void publishPaymentFailed_PublishesEvent() {
        publisher.publishPaymentFailed(testPayment);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("payment-events"), eq(testPayment.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("PAYMENT_FAILED");
    }

    @Test
    void publishPaymentCancelled_PublishesEvent() {
        publisher.publishPaymentCancelled(testPayment);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("payment-events"), eq(testPayment.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("PAYMENT_CANCELLED");
    }
}
