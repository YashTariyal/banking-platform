package com.banking.account.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI accountServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Account Service API")
                        .version("v1.0.0")
                        .description("""
                                REST API for managing customer accounts in the banking platform.
                                
                                This service provides endpoints for:
                                - Creating and managing customer accounts
                                - Processing account transactions (credits and debits)
                                - Querying account balances and transaction history
                                - Updating account details and status
                                
                                All transactions are idempotent and protected with optimistic locking.
                                """)
                        .contact(new Contact()
                                .name("Banking Platform Team")
                                .email("platform@example.com")
                                .url("https://github.com/banking-platform"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://banking-platform.com/license")))
                .tags(List.of(
                        new Tag().name("Accounts").description("Account lifecycle management operations"),
                        new Tag().name("Transactions").description("Account transaction operations"),
                        new Tag().name("Balance").description("Account balance inquiry operations")
                ));
    }
}

