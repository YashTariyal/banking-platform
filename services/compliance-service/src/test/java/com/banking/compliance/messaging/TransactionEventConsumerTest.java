package com.banking.compliance.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.compliance.domain.ComplianceRecord;
import com.banking.compliance.service.AMLService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class TransactionEventConsumerTest {

    @Mock
    private AMLService amlService;

    @Mock
    private Acknowledgment acknowledgment;

    private ObjectMapper objectMapper;
    private TransactionEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new TransactionEventConsumer(amlService, objectMapper);
    }

    @Test
    void consumeTransactionEvent_WithValidEvent_ProcessesTransaction() throws Exception {
        // Given
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("15000.00");

        String eventJson = String.format(
                "{\"eventType\":\"TRANSACTION_COMPLETED\",\"customerId\":\"%s\",\"accountId\":\"%s\",\"transactionId\":\"%s\",\"amount\":\"%s\",\"currency\":\"USD\"}",
                customerId, accountId, transactionId, amount
        );

        ComplianceRecord record = new ComplianceRecord();
        record.setId(UUID.randomUUID());
        when(amlService.analyzeTransaction(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(record);

        // When
        consumer.consumeTransactionEvent(eventJson, "transaction-events", transactionId.toString(), acknowledgment);

        // Then
        ArgumentCaptor<UUID> customerIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<UUID> accountIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<UUID> transactionIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<String> currencyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);

        verify(amlService).analyzeTransaction(
                customerIdCaptor.capture(),
                accountIdCaptor.capture(),
                transactionIdCaptor.capture(),
                amountCaptor.capture(),
                currencyCaptor.capture(),
                eventTypeCaptor.capture(),
                topicCaptor.capture()
        );

        assertThat(customerIdCaptor.getValue()).isEqualTo(customerId);
        assertThat(accountIdCaptor.getValue()).isEqualTo(accountId);
        assertThat(transactionIdCaptor.getValue()).isEqualTo(transactionId);
        assertThat(amountCaptor.getValue()).isEqualByComparingTo(amount);
        assertThat(currencyCaptor.getValue()).isEqualTo("USD");
        assertThat(eventTypeCaptor.getValue()).isEqualTo("TRANSACTION_COMPLETED");
        assertThat(topicCaptor.getValue()).isEqualTo("transaction-events");

        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeTransactionEvent_WithMissingCustomerId_SkipsProcessing() throws Exception {
        // Given
        String eventJson = "{\"eventType\":\"TRANSACTION_COMPLETED\",\"amount\":\"1000.00\",\"currency\":\"USD\"}";

        // When
        consumer.consumeTransactionEvent(eventJson, "transaction-events", "key", acknowledgment);

        // Then
        verify(amlService, never()).analyzeTransaction(any(), any(), any(), any(), any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeTransactionEvent_WithMissingAmount_SkipsProcessing() throws Exception {
        // Given
        UUID customerId = UUID.randomUUID();
        String eventJson = String.format(
                "{\"eventType\":\"TRANSACTION_COMPLETED\",\"customerId\":\"%s\",\"currency\":\"USD\"}",
                customerId
        );

        // When
        consumer.consumeTransactionEvent(eventJson, "transaction-events", "key", acknowledgment);

        // Then
        verify(amlService, never()).analyzeTransaction(any(), any(), any(), any(), any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeTransactionEvent_WithInvalidJson_AcknowledgesAndContinues() {
        // Given
        String invalidJson = "{invalid json}";

        // When
        consumer.consumeTransactionEvent(invalidJson, "transaction-events", "key", acknowledgment);

        // Then
        verify(amlService, never()).analyzeTransaction(any(), any(), any(), any(), any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeTransactionEvent_WithPaymentEvent_ProcessesCorrectly() throws Exception {
        // Given
        UUID customerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("5000.00");

        String eventJson = String.format(
                "{\"eventType\":\"PAYMENT_PROCESSED\",\"customerId\":\"%s\",\"amount\":\"%s\",\"currency\":\"EUR\"}",
                customerId, amount
        );

        ComplianceRecord record = new ComplianceRecord();
        when(amlService.analyzeTransaction(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(record);

        // When
        consumer.consumeTransactionEvent(eventJson, "payment-events", "key", acknowledgment);

        // Then
        verify(amlService).analyzeTransaction(
                eq(customerId),
                any(),
                any(),
                eq(amount),
                eq("EUR"),
                eq("PAYMENT_PROCESSED"),
                eq("payment-events")
        );
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeTransactionEvent_WithCardEvent_ProcessesCorrectly() throws Exception {
        // Given
        UUID customerId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("2000.00");

        String eventJson = String.format(
                "{\"eventType\":\"CARD_TRANSACTION\",\"customerId\":\"%s\",\"cardId\":\"%s\",\"amount\":\"%s\",\"currency\":\"USD\"}",
                customerId, cardId, amount
        );

        ComplianceRecord record = new ComplianceRecord();
        when(amlService.analyzeTransaction(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(record);

        // When
        consumer.consumeTransactionEvent(eventJson, "card-events", cardId.toString(), acknowledgment);

        // Then
        verify(amlService).analyzeTransaction(
                eq(customerId),
                any(),
                any(),
                eq(amount),
                eq("USD"),
                eq("CARD_TRANSACTION"),
                eq("card-events")
        );
        verify(acknowledgment).acknowledge();
    }
}

