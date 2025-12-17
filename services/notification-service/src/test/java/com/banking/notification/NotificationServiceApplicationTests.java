package com.banking.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "eureka.client.enabled=false",
    "notification.email.enabled=false",
    "notification.sms.enabled=false",
    "notification.push.enabled=false"
})
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
