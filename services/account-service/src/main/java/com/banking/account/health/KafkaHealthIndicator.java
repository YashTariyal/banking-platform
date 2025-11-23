package com.banking.account.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Custom health indicator for Kafka connectivity.
 */
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(KafkaHealthIndicator.class);
    private final KafkaTemplate<?, ?> kafkaTemplate;
    private final String bootstrapServers;

    public KafkaHealthIndicator(KafkaTemplate<?, ?> kafkaTemplate, 
                                KafkaAdmin kafkaAdmin) {
        this.kafkaTemplate = kafkaTemplate;
        // Extract bootstrap servers from KafkaAdmin
        this.bootstrapServers = extractBootstrapServers(kafkaAdmin);
    }

    @Override
    public Health health() {
        try {
            // Check if KafkaTemplate is available
            if (kafkaTemplate == null) {
                return Health.down()
                        .withDetail("kafka", "Not configured")
                        .withDetail("status", "Unavailable")
                        .build();
            }

            // Try to list topics to verify connectivity
            Properties props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
            props.put(AdminClientConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 10000);

            try (AdminClient adminClient = AdminClient.create(props)) {
                ListTopicsResult topics = adminClient.listTopics();
                topics.names().get(5, TimeUnit.SECONDS);
                
                return Health.up()
                        .withDetail("kafka", "Connected")
                        .withDetail("bootstrapServers", bootstrapServers)
                        .withDetail("status", "Available")
                        .build();
            }
        } catch (Exception ex) {
            log.error("Kafka health check failed", ex);
            return Health.down()
                    .withDetail("kafka", "Disconnected")
                    .withDetail("bootstrapServers", bootstrapServers)
                    .withDetail("status", "Unavailable")
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }

    private String extractBootstrapServers(KafkaAdmin kafkaAdmin) {
        try {
            var configs = kafkaAdmin.getConfigurationProperties();
            Object servers = configs.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);
            return servers != null ? servers.toString() : "localhost:9092";
        } catch (Exception e) {
            log.warn("Could not extract bootstrap servers from KafkaAdmin, using default", e);
            return "localhost:9092";
        }
    }
}

