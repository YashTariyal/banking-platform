package com.banking.document;

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
    "document.storage.base-path=/tmp/test-documents",
    "document.allowed-types=application/pdf,image/jpeg,image/png"
})
class DocumentServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
