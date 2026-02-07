package com.example.contractfirst.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom health indicator that adds timestamp to health endpoint.
 *
 * Required by Java Developer Agent specifications:
 * All health responses must include timestamp field.
 *
 * Accessible at: /actuator/health
 *
 * @author Wallace Espindola
 */
@Component
public class HealthConfig implements HealthIndicator {

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        details.put("timestamp", Instant.now().toString());
        details.put("status", "UP");
        details.put("application", "contract-first-integrations");

        return Health.up()
                .withDetails(details)
                .build();
    }
}
