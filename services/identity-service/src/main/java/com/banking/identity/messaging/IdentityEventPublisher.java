package com.banking.identity.messaging;

import com.banking.identity.domain.User;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class IdentityEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(IdentityEventPublisher.class);
    private static final String IDENTITY_EVENTS_TOPIC = "identity-events";

    private final KafkaTemplate<String, IdentityEvent> kafkaTemplate;

    public IdentityEventPublisher(KafkaTemplate<String, IdentityEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserRegistered(User user) {
        publishEvent(IdentityEvent.userRegistered(user.getId(), user.getUsername(), user.getCustomerId()),
                user.getId().toString());
    }

    public void publishUserLoggedIn(User user) {
        publishEvent(IdentityEvent.userLoggedIn(user.getId(), user.getUsername(), user.getCustomerId()),
                user.getId().toString());
    }

    public void publishUserLoggedOut(UUID userId) {
        publishEvent(IdentityEvent.userLoggedOut(userId), userId.toString());
    }

    public void publishUserLocked(User user) {
        publishEvent(IdentityEvent.userLocked(user.getId(), user.getUsername(), user.getCustomerId()),
                user.getId().toString());
    }

    private void publishEvent(IdentityEvent event, String key) {
        try {
            kafkaTemplate.send(IDENTITY_EVENTS_TOPIC, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} event for user {}", event.eventType(), event.userId(), ex);
                        } else {
                            log.debug("Published {} event for user {}", event.eventType(), event.userId());
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing {} event for user {}", event.eventType(), event.userId(), e);
        }
    }
}

