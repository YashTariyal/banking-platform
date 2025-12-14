package com.banking.identity.messaging;

import com.banking.identity.domain.User;
import com.banking.identity.domain.UserStatus;
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
class IdentityEventPublisherTest {

    @Mock
    private KafkaTemplate<String, IdentityEvent> kafkaTemplate;

    private IdentityEventPublisher publisher;
    private User testUser;

    @BeforeEach
    void setUp() {
        publisher = new IdentityEventPublisher(kafkaTemplate);
        
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setCustomerId(UUID.randomUUID());
        testUser.setStatus(UserStatus.ACTIVE);

        CompletableFuture<SendResult<String, IdentityEvent>> future = new CompletableFuture<>();
        future.complete(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(IdentityEvent.class)))
                .thenReturn(future);
    }

    @Test
    void publishUserRegistered_PublishesEvent() {
        publisher.publishUserRegistered(testUser);

        ArgumentCaptor<IdentityEvent> eventCaptor = ArgumentCaptor.forClass(IdentityEvent.class);
        verify(kafkaTemplate).send(eq("identity-events"), eq(testUser.getId().toString()), eventCaptor.capture());

        assertThat(eventCaptor.getValue().eventType()).isEqualTo("USER_REGISTERED");
        assertThat(eventCaptor.getValue().userId()).isEqualTo(testUser.getId());
        assertThat(eventCaptor.getValue().username()).isEqualTo("testuser");
    }

    @Test
    void publishUserLoggedIn_PublishesEvent() {
        publisher.publishUserLoggedIn(testUser);

        ArgumentCaptor<IdentityEvent> eventCaptor = ArgumentCaptor.forClass(IdentityEvent.class);
        verify(kafkaTemplate).send(eq("identity-events"), eq(testUser.getId().toString()), eventCaptor.capture());

        assertThat(eventCaptor.getValue().eventType()).isEqualTo("USER_LOGGED_IN");
    }

    @Test
    void publishUserLoggedOut_PublishesEvent() {
        publisher.publishUserLoggedOut(testUser.getId());

        ArgumentCaptor<IdentityEvent> eventCaptor = ArgumentCaptor.forClass(IdentityEvent.class);
        verify(kafkaTemplate).send(eq("identity-events"), eq(testUser.getId().toString()), eventCaptor.capture());

        assertThat(eventCaptor.getValue().eventType()).isEqualTo("USER_LOGGED_OUT");
    }

    @Test
    void publishUserLocked_PublishesEvent() {
        publisher.publishUserLocked(testUser);

        ArgumentCaptor<IdentityEvent> eventCaptor = ArgumentCaptor.forClass(IdentityEvent.class);
        verify(kafkaTemplate).send(eq("identity-events"), eq(testUser.getId().toString()), eventCaptor.capture());

        assertThat(eventCaptor.getValue().eventType()).isEqualTo("USER_LOCKED");
    }
}
