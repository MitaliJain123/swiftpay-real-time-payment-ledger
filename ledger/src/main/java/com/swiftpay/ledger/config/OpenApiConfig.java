package com.swiftpay.ledger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ledgerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftPay Ledger Service API")
                        .description("""
                                Settles payments from Kafka payment-initiated events \
                                (double-entry ledger) and exposes transaction history \
                                for users. Publishes payment-completed / payment-failed events.
                                """)
                        .version("v1")
                        .contact(new Contact()
                                .name("SwiftPay")
                                .email("dev@swiftpay.local"))
                        .license(new License().name("Apache 2.0")));
    }
}
