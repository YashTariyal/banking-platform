package com.banking.card.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cardServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Card Service API")
                        .version("v1.0.0")
                        .description("""
                                REST API for managing payment cards in the banking platform.
                                
                                This service provides endpoints for:
                                - Card lifecycle management (issuance, activation, blocking, cancellation)
                                - Card controls and limits (spending limits, transaction limits, ATM limits)
                                - PIN management and security features
                                - Transaction authorization and fraud detection
                                - Card-to-card transfers and restrictions
                                - Analytics and reporting
                                
                                All endpoints require JWT Bearer token authentication.
                                
                                Swagger tips:
                                - Click the green \"Authorize\" button and paste your JWT without the 'Bearer ' prefix.
                                - For local testing, you can mint a dev token via POST /api/auth/token (set card.security.dev-token.enabled=true).
                                """)
                        .contact(new Contact()
                                .name("Banking Platform Team")
                                .email("platform@example.com")
                                .url("https://github.com/banking-platform"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://banking-platform.com/license")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("""
                                                Enter your JWT token (without 'Bearer' prefix).
                                                For local dev you can call POST /api/auth/token to mint a token, or generate at jwt.io using the configured secret.
                                                """)))
                .tags(List.of(
                        new Tag().name("Cards").description("Card lifecycle management operations"),
                        new Tag().name("Transactions").description("Card transaction operations"),
                        new Tag().name("Authorization").description("Transaction authorization operations"),
                        new Tag().name("Balance Verification").description("Balance verification operations"),
                        new Tag().name("Fraud Detection").description("Fraud detection and monitoring"),
                        new Tag().name("Restrictions").description("Merchant and geographic restrictions"),
                        new Tag().name("Transfers").description("Card-to-card transfer operations"),
                        new Tag().name("Notifications").description("Card notification management"),
                        new Tag().name("Card Analytics").description("Card usage analytics and reporting"),
                        new Tag().name("Authentication").description("Authentication and identity utilities")
                ));
    }
}

