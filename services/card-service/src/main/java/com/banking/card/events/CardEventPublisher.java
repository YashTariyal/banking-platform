package com.banking.card.events;

import com.banking.card.domain.Card;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CardEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CardEventPublisher.class);
    private static final String CARD_EVENTS_TOPIC = "card-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public CardEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishCardIssued(Card card) {
        publishEvent("CardIssued", card.getId(), card.getCustomerId(), Map.of(
                "cardType", card.getType().name(),
                "currency", card.getCurrency(),
                "status", card.getStatus().name()
        ));
    }

    public void publishCardActivated(Card card) {
        publishEvent("CardActivated", card.getId(), card.getCustomerId(), Map.of(
                "status", card.getStatus().name()
        ));
    }

    public void publishCardBlocked(Card card) {
        publishEvent("CardBlocked", card.getId(), card.getCustomerId(), Map.of(
                "status", card.getStatus().name()
        ));
    }

    public void publishCardCancelled(Card card, String reason) {
        publishEvent("CardCancelled", card.getId(), card.getCustomerId(), Map.of(
                "status", card.getStatus().name(),
                "reason", reason
        ));
    }

    public void publishCardFrozen(Card card, String reason) {
        publishEvent("CardFrozen", card.getId(), card.getCustomerId(), Map.of(
                "reason", reason != null ? reason : "No reason provided"
        ));
    }

    public void publishCardUnfrozen(Card card) {
        publishEvent("CardUnfrozen", card.getId(), card.getCustomerId(), Map.of());
    }

    public void publishCardReplaced(Card oldCard, Card newCard, String reason) {
        publishEvent("CardReplaced", newCard.getId(), newCard.getCustomerId(), Map.of(
                "oldCardId", oldCard.getId().toString(),
                "reason", reason
        ));
    }

    public void publishCardRenewed(Card oldCard, Card newCard) {
        publishEvent("CardRenewed", newCard.getId(), newCard.getCustomerId(), Map.of(
                "oldCardId", oldCard.getId().toString(),
                "renewalCount", newCard.getRenewalCount() != null ? newCard.getRenewalCount() : 0
        ));
    }

    public void publishPinChanged(Card card) {
        publishEvent("PinChanged", card.getId(), card.getCustomerId(), Map.of());
    }

    public void publishCvvRotated(Card card) {
        publishEvent("CvvRotated", card.getId(), card.getCustomerId(), Map.of());
    }

    public void publishLimitUpdated(Card card, String limitType, String oldValue, String newValue) {
        publishEvent("LimitUpdated", card.getId(), card.getCustomerId(), Map.of(
                "limitType", limitType,
                "oldValue", oldValue,
                "newValue", newValue
        ));
    }

    private void publishEvent(String eventType, UUID cardId, UUID customerId, Map<String, Object> additionalData) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("cardId", cardId.toString());
            event.put("customerId", customerId.toString());
            event.put("timestamp", Instant.now().toString());
            event.putAll(additionalData);

            String payload = objectMapper.writeValueAsString(event);
            String key = cardId != null ? cardId.toString() : "unknown";

            kafkaTemplate.send(CARD_EVENTS_TOPIC, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} event for card {}", eventType, cardId, ex);
                        } else {
                            log.debug("Published {} event for card {}", eventType, cardId);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} event for card {}", eventType, cardId, e);
        }
    }
}

