package com.example.contractfirst.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Java Record DTO for create order request.
 *
 * Maps to OpenAPI contract: contracts/openapi/orders-api.v1.yaml
 *
 * @author Wallace Espindola
 */
public record CreateOrderRequest(
        @NotBlank(message = "customerId is required")
        String customerId,

        String idempotencyKey, // Optional for safe retries

        @NotEmpty(message = "items must not be empty")
        @Valid
        List<OrderItem> items
) {
}
