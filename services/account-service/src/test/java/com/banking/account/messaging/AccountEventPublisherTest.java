package com.banking.account.messaging;

import com.banking.account.config.AccountTopicProperties;
import com.banking.account.domain.Account;
import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountType;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountEventPublisherTest {

    @Mock
    private KafkaTemplate<String, AccountEvent> kafkaTemplate;

    @Mock
    private AccountTopicProperties topicProperties;

    @Mock
    private FailedEventRetryService failedEventRetryService;

    private AccountEventPublisher publisher;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        publisher = new AccountEventPublisher(kafkaTemplate, topicProperties, failedEventRetryService);
        
        testAccount = new Account();
        testAccount.setId(UUID.randomUUID());
        testAccount.setAccountNumber("ACC123456789012");
        testAccount.setCustomerId(UUID.randomUUID());
        testAccount.setType(AccountType.CHECKING);
        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setCurrency("USD");
        testAccount.setBalance(new BigDecimal("1000.00"));

        when(topicProperties.getAccountCreated()).thenReturn("account-events");
        when(topicProperties.getAccountUpdated()).thenReturn("account-events");

        CompletableFuture<SendResult<String, AccountEvent>> future = new CompletableFuture<>();
        future.complete(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(AccountEvent.class)))
                .thenReturn(future);
    }

    @Test
    void publishAccountCreated_PublishesEvent() {
        publisher.publishAccountCreated(testAccount);

        ArgumentCaptor<AccountEvent> eventCaptor = ArgumentCaptor.forClass(AccountEvent.class);
        verify(kafkaTemplate).send(eq("account-events"), eq(testAccount.getId().toString()), eventCaptor.capture());

        assertThat(eventCaptor.getValue().eventType()).isEqualTo("ACCOUNT_CREATED");
        assertThat(eventCaptor.getValue().accountId()).isEqualTo(testAccount.getId());
    }

    @Test
    void publishAccountUpdated_PublishesEvent() {
        publisher.publishAccountUpdated(testAccount);

        ArgumentCaptor<AccountEvent> eventCaptor = ArgumentCaptor.forClass(AccountEvent.class);
        verify(kafkaTemplate).send(eq("account-events"), eq(testAccount.getId().toString()), eventCaptor.capture());

        assertThat(eventCaptor.getValue().eventType()).isEqualTo("ACCOUNT_UPDATED");
    }
}
