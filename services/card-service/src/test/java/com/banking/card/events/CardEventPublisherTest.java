package com.banking.card.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.card.domain.Card;
import com.banking.card.domain.CardStatus;
import com.banking.card.domain.CardType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
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

@ExtendWith(MockitoExtension.class)
class CardEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;
    private CardEventPublisher eventPublisher;
    private Card testCard;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        eventPublisher = new CardEventPublisher(kafkaTemplate, objectMapper);
        
        testCard = new Card();
        testCard.setId(UUID.randomUUID());
        testCard.setCustomerId(UUID.randomUUID());
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setType(CardType.DEBIT);
        testCard.setCurrency("USD");
        
        // Mock KafkaTemplate.send() to return a CompletableFuture
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.complete(null); // Complete immediately for testing
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenReturn(future);
    }

    @Test
    void publishCardIssued() {
        eventPublisher.publishCardIssued(testCard);
        
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());
        
        assert topicCaptor.getValue().equals("card-events");
        assert keyCaptor.getValue().equals(testCard.getId().toString());
        assert payloadCaptor.getValue().contains("CardIssued");
        assert payloadCaptor.getValue().contains(testCard.getId().toString());
    }

    @Test
    void publishCardActivated() {
        eventPublisher.publishCardActivated(testCard);
        
        verify(kafkaTemplate).send(eq("card-events"), eq(testCard.getId().toString()), any(String.class));
    }

    @Test
    void publishCardBlocked() {
        eventPublisher.publishCardBlocked(testCard);
        
        verify(kafkaTemplate).send(eq("card-events"), eq(testCard.getId().toString()), any(String.class));
    }

    @Test
    void publishCardCancelled() {
        String reason = "Customer request";
        eventPublisher.publishCardCancelled(testCard, reason);
        
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("card-events"), eq(testCard.getId().toString()), payloadCaptor.capture());
        
        assert payloadCaptor.getValue().contains("CardCancelled");
        assert payloadCaptor.getValue().contains(reason);
    }

    @Test
    void publishCardFrozen() {
        String reason = "Suspicious activity";
        eventPublisher.publishCardFrozen(testCard, reason);
        
        verify(kafkaTemplate).send(eq("card-events"), eq(testCard.getId().toString()), any(String.class));
    }

    @Test
    void publishCardUnfrozen() {
        eventPublisher.publishCardUnfrozen(testCard);
        
        verify(kafkaTemplate).send(eq("card-events"), eq(testCard.getId().toString()), any(String.class));
    }

    @Test
    void publishPinChanged() {
        eventPublisher.publishPinChanged(testCard);
        
        verify(kafkaTemplate).send(eq("card-events"), eq(testCard.getId().toString()), any(String.class));
    }

    @Test
    void publishCvvRotated() {
        eventPublisher.publishCvvRotated(testCard);
        
        verify(kafkaTemplate).send(eq("card-events"), eq(testCard.getId().toString()), any(String.class));
    }

    @Test
    void publishLimitUpdated() {
        eventPublisher.publishLimitUpdated(testCard, "SPENDING_LIMIT", "1000", "2000");
        
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("card-events"), eq(testCard.getId().toString()), payloadCaptor.capture());
        
        assert payloadCaptor.getValue().contains("LimitUpdated");
        assert payloadCaptor.getValue().contains("SPENDING_LIMIT");
    }

    @Test
    void publishCardReplaced() {
        Card oldCard = new Card();
        oldCard.setId(UUID.randomUUID());
        oldCard.setCustomerId(testCard.getCustomerId());
        
        Card newCard = testCard;
        String reason = "Lost card";
        
        eventPublisher.publishCardReplaced(oldCard, newCard, reason);
        
        verify(kafkaTemplate).send(eq("card-events"), eq(newCard.getId().toString()), any(String.class));
    }

    @Test
    void publishCardRenewed() {
        Card oldCard = new Card();
        oldCard.setId(UUID.randomUUID());
        oldCard.setCustomerId(testCard.getCustomerId());
        
        Card newCard = testCard;
        newCard.setRenewalCount(1);
        
        eventPublisher.publishCardRenewed(oldCard, newCard);
        
        verify(kafkaTemplate).send(eq("card-events"), eq(newCard.getId().toString()), any(String.class));
    }
}

