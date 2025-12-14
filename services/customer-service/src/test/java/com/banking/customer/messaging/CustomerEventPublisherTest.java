package com.banking.customer.messaging;

import com.banking.customer.domain.Customer;
import com.banking.customer.domain.CustomerStatus;
import com.banking.customer.domain.CustomerType;
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
class CustomerEventPublisherTest {

    @Mock
    private KafkaTemplate<String, CustomerEvent> kafkaTemplate;

    private CustomerEventPublisher publisher;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        publisher = new CustomerEventPublisher(kafkaTemplate);
        
        testCustomer = new Customer();
        testCustomer.setId(UUID.randomUUID());
        testCustomer.setCustomerNumber("CUST123456789012");
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
        testCustomer.setCustomerType(CustomerType.INDIVIDUAL);
        testCustomer.setStatus(CustomerStatus.ACTIVE);
        testCustomer.setKycStatus("VERIFIED");

        CompletableFuture<SendResult<String, CustomerEvent>> future = new CompletableFuture<>();
        future.complete(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(CustomerEvent.class)))
                .thenReturn(future);
    }

    @Test
    void publishCustomerCreated_PublishesEvent() {
        publisher.publishCustomerCreated(testCustomer);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CustomerEvent> eventCaptor = ArgumentCaptor.forClass(CustomerEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("customer-events");
        assertThat(keyCaptor.getValue()).isEqualTo(testCustomer.getId().toString());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo("CUSTOMER_CREATED");
        assertThat(eventCaptor.getValue().customerId()).isEqualTo(testCustomer.getId());
    }

    @Test
    void publishCustomerUpdated_PublishesEvent() {
        publisher.publishCustomerUpdated(testCustomer);

        ArgumentCaptor<CustomerEvent> eventCaptor = ArgumentCaptor.forClass(CustomerEvent.class);
        verify(kafkaTemplate).send(eq("customer-events"), eq(testCustomer.getId().toString()), eventCaptor.capture());

        assertThat(eventCaptor.getValue().eventType()).isEqualTo("CUSTOMER_UPDATED");
    }

    @Test
    void publishCustomerDeleted_PublishesEvent() {
        publisher.publishCustomerDeleted(testCustomer);

        ArgumentCaptor<CustomerEvent> eventCaptor = ArgumentCaptor.forClass(CustomerEvent.class);
        verify(kafkaTemplate).send(eq("customer-events"), eq(testCustomer.getId().toString()), eventCaptor.capture());

        assertThat(eventCaptor.getValue().eventType()).isEqualTo("CUSTOMER_DELETED");
    }
}
