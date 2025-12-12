package com.banking.customer.config;

import com.banking.customer.config.PiiMaskingFilter;
import com.banking.customer.config.RequestLoggingFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "customer.security.enabled=false",
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Import({RequestLoggingFilter.class, PiiMaskingFilter.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenSecurityDisabled_allowsAccessWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class MeterRegistryConfig {
        @Bean
        SimpleMeterRegistry simpleMeterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
