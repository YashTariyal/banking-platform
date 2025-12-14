package com.banking.card.events;

import com.banking.card.domain.FraudSeverity;
import java.math.BigDecimal;
import java.util.List;
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
class FraudEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private FraudEventPublisher publisher;
    private UUID testCardId;
    private UUID testCustomerId;

    @BeforeEach
    void setUp() {
        publisher = new FraudEventPublisher(kafkaTemplate, new com.fasterxml.jackson.databind.ObjectMapper());
        testCardId = UUID.randomUUID();
        testCustomerId = UUID.randomUUID();

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.complete(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenReturn(future);
    }

    @Test
    void publishFraudDetected_PublishesEvent() {
        FraudSeverity severity = FraudSeverity.HIGH;
        BigDecimal fraudScore = new BigDecimal("85.5");
        List<String> riskFactors = List.of("Unusual location", "Large amount");

        publisher.publishFraudDetected(testCardId, testCustomerId, severity, fraudScore, riskFactors);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("card-fraud-events"), eq(testCardId.toString()), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).contains("FraudDetected");
        assertThat(payloadCaptor.getValue()).contains(testCardId.toString());
        assertThat(payloadCaptor.getValue()).contains("HIGH");
        assertThat(payloadCaptor.getValue()).contains("85.5");
    }
}
