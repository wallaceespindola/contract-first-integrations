package com.example.contractfirst.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Maps to HTTP 404 status code.
 *
 * @author Wallace Espindola
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
