package com.banking.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "eureka.client.enabled=false",
    "health.check-interval-seconds=300"
})
class HealthAggregatorApplicationTests {

    @Test
    void contextLoads() {
    }
}
