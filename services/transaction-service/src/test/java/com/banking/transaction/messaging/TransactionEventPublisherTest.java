package com.banking.transaction.messaging;

import com.banking.transaction.domain.Transaction;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.domain.TransactionType;
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
class TransactionEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private TransactionEventPublisher publisher;
    private Transaction testTransaction;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        publisher = new TransactionEventPublisher(kafkaTemplate, objectMapper, fixedClock);
        
        testTransaction = new Transaction();
        testTransaction.setId(UUID.randomUUID());
        testTransaction.setReferenceId(UUID.randomUUID().toString());
        testTransaction.setTransactionType(TransactionType.DEPOSIT);
        testTransaction.setStatus(TransactionStatus.COMPLETED);
        testTransaction.setCustomerId(UUID.randomUUID());
        testTransaction.setFromAccountId(UUID.randomUUID());
        testTransaction.setAmount(new BigDecimal("1000.00"));
        testTransaction.setCurrency("USD");
        testTransaction.setDescription("Test transaction");

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.complete(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenReturn(future);
    }

    @Test
    void publishTransactionInitiated_PublishesEvent() {
        publisher.publishTransactionInitiated(testTransaction);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("transaction-events"), eq(testTransaction.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("TRANSACTION_INITIATED");
        assertThat(payloadCaptor.getValue()).contains(testTransaction.getId().toString());
    }

    @Test
    void publishTransactionProcessing_PublishesEvent() {
        publisher.publishTransactionProcessing(testTransaction);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("transaction-events"), eq(testTransaction.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("TRANSACTION_PROCESSING");
    }

    @Test
    void publishTransactionCompleted_PublishesEvent() {
        publisher.publishTransactionCompleted(testTransaction);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("transaction-events"), eq(testTransaction.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("TRANSACTION_COMPLETED");
    }

    @Test
    void publishTransactionFailed_PublishesEvent() {
        publisher.publishTransactionFailed(testTransaction);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("transaction-events"), eq(testTransaction.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("TRANSACTION_FAILED");
    }

    @Test
    void publishTransactionCancelled_PublishesEvent() {
        publisher.publishTransactionCancelled(testTransaction);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("transaction-events"), eq(testTransaction.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("TRANSACTION_CANCELLED");
    }

    @Test
    void publishTransactionReversed_PublishesEvent() {
        publisher.publishTransactionReversed(testTransaction);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("transaction-events"), eq(testTransaction.getId().toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("TRANSACTION_REVERSED");
    }
}
