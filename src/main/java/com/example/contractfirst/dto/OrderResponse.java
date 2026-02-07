package com.example.contractfirst.dto;

import java.time.Instant;
import java.util.List;

/**
 * Java Record DTO for order response.
 *
 * Maps to OpenAPI contract: contracts/openapi/orders-api.v1.yaml
 * Includes timestamp field (Java Developer Agent requirement).
 *
 * @author Wallace Espindola
 */
public record OrderResponse(
        String orderId,
        String customerId,
        String status,
        List<OrderItem> items,
        Instant timestamp
) {
}
