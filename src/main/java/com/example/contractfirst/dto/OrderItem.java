package com.example.contractfirst.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Java Record DTO for order item.
 *
 * Maps to OpenAPI contract: contracts/openapi/orders-api.v1.yaml
 *
 * @author Wallace Espindola
 */
public record OrderItem(
        @NotBlank(message = "sku is required")
        String sku,

        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity
) {
}
