package com.banking.customer.messaging;

import com.banking.customer.domain.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CustomerEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CustomerEventPublisher.class);
    private static final String CUSTOMER_EVENTS_TOPIC = "customer-events";

    private final KafkaTemplate<String, CustomerEvent> kafkaTemplate;

    public CustomerEventPublisher(KafkaTemplate<String, CustomerEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCustomerCreated(Customer customer) {
        publishEvent(CustomerEvent.created(customer), customer.getId().toString());
    }

    public void publishCustomerUpdated(Customer customer) {
        publishEvent(CustomerEvent.updated(customer), customer.getId().toString());
    }

    public void publishCustomerDeleted(Customer customer) {
        publishEvent(CustomerEvent.deleted(customer), customer.getId().toString());
    }

    private void publishEvent(CustomerEvent event, String key) {
        try {
            kafkaTemplate.send(CUSTOMER_EVENTS_TOPIC, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} event for customer {}", event.eventType(), event.customerId(), ex);
                        } else {
                            log.debug("Published {} event for customer {}", event.eventType(), event.customerId());
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing {} event for customer {}", event.eventType(), event.customerId(), e);
        }
    }
}

