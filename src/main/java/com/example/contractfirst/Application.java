package com.example.contractfirst;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Main Spring Boot application for contract-first integrations.
 *
 * Demonstrates contract-first patterns with:
 * - REST API (OpenAPI contracts)
 * - Kafka Events (Avro schemas)
 * - Database (Flyway migrations)
 *
 * @author Wallace Espindola
 * @see <a href="https://github.com/wallaceespindola">GitHub</a>
 */
@SpringBootApplication
@EnableKafka
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
