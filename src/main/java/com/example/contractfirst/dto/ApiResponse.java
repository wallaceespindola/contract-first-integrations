package com.example.contractfirst.dto;

import java.time.Instant;

/**
 * Generic API response wrapper with timestamp.
 *
 * Java Developer Agent requirement: all responses must include timestamp.
 *
 * @author Wallace Espindola
 */
public record ApiResponse<T>(
        T data,
        String status,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, "success", Instant.now());
    }

    public static <T> ApiResponse<T> error(T data) {
        return new ApiResponse<>(data, "error", Instant.now());
    }
}
