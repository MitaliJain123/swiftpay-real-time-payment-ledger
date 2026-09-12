package com.swiftpay.analytics.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI analyticsWorkerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftPay Analytics Worker API")
                        .description("""
                                Consumes payment-completed Kafka events into ClickHouse \
                                and exposes real-time payment volume monitoring endpoints.
                                """)
                        .version("v1")
                        .contact(new Contact()
                                .name("SwiftPay")
                                .email("dev@swiftpay.local"))
                        .license(new License().name("Apache 2.0")));
    }
}
