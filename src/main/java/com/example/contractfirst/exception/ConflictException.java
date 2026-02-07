package com.example.contractfirst.exception;

/**
 * Exception thrown when there's a conflict (e.g., idempotency key reused with different payload).
 * Maps to HTTP 409 status code.
 *
 * @author Wallace Espindola
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
