package com.swiftpay.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transactionGatewayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftPay Transaction Gateway API")
                        .description("""
                                Accepts peer-to-peer payment requests, enforces Redis \
                                idempotency, persists a PENDING payment, and publishes \
                                a payment-initiated Kafka event for the Ledger Service.
                                """)
                        .version("v1")
                        .contact(new Contact()
                                .name("SwiftPay")
                                .email("dev@swiftpay.local"))
                        .license(new License().name("Apache 2.0")));
    }
}
