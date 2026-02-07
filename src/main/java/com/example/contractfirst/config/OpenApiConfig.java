package com.example.contractfirst.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for REST API documentation.
 *
 * Accessible at:
 * - Swagger UI: /swagger-ui.html
 * - OpenAPI spec: /v3/api-docs
 *
 * @author Wallace Espindola
 */
@Configuration
public class OpenApiConfig {

    @Value("${info.app.name:Contract-First Integrations}")
    private String appName;

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    @Value("${info.app.description:Reference implementation demonstrating contract-first patterns}")
    private String appDescription;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(appName)
                        .version(appVersion)
                        .description(appDescription + " - REST API with idempotency, Kafka events with Avro, and Flyway database migrations")
                        .contact(new Contact()
                                .name("Wallace Espindola")
                                .email("wallace.espindola@gmail.com")
                                .url("https://github.com/wallaceespindola"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development"),
                        new Server()
                                .url("https://api.example.com")
                                .description("Production")));
    }
}
