package com.example.contractfirst.exception;

import com.example.contractfirst.dto.ErrorResponse;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for REST API.
 *
 * <p>Maps exceptions to standardized ErrorResponse DTOs with proper HTTP status codes. All error
 * responses include traceId and timestamp.
 *
 * @author Wallace Espindola
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex) {
    String traceId = UUID.randomUUID().toString();
    String message =
        ex.getBindingResult().getAllErrors().stream()
            .map(
                error -> {
                  if (error instanceof FieldError fieldError) {
                    return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                  }
                  return error.getDefaultMessage();
                })
            .collect(Collectors.joining(", "));

    log.error("Validation error [traceId={}]: {}", traceId, message);

    ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", message, traceId, Instant.now());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
      ResourceNotFoundException ex) {
    String traceId = UUID.randomUUID().toString();
    log.error("Resource not found [traceId={}]: {}", traceId, ex.getMessage());

    ErrorResponse error = new ErrorResponse("NOT_FOUND", ex.getMessage(), traceId, Instant.now());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex) {
    String traceId = UUID.randomUUID().toString();
    log.error("Conflict [traceId={}]: {}", traceId, ex.getMessage());

    ErrorResponse error = new ErrorResponse("CONFLICT", ex.getMessage(), traceId, Instant.now());

    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    String traceId = UUID.randomUUID().toString();
    log.error("Internal error [traceId={}]: {}", traceId, ex.getMessage(), ex);

    ErrorResponse error =
        new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please contact support with traceId: " + traceId,
            traceId,
            Instant.now());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
