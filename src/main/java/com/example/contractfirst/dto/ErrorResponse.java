package com.example.contractfirst.dto;

import java.time.Instant;

/**
 * Java Record DTO for error response.
 *
 * Maps to OpenAPI contract: contracts/openapi/orders-api.v1.yaml
 * Includes timestamp and traceId for correlation.
 *
 * @author Wallace Espindola
 */
public record ErrorResponse(
        String code,
        String message,
        String traceId,
        Instant timestamp
) {
}
