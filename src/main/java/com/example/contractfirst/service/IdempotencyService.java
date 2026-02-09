package com.example.contractfirst.service;

import com.example.contractfirst.entity.IdempotencyKeyEntity;
import com.example.contractfirst.exception.ConflictException;
import com.example.contractfirst.repository.IdempotencyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for REST API idempotency management.
 *
 * <p>Implements idempotency pattern: - Stores idempotency keys with request payload hash - Returns
 * cached result if key exists with same hash (safe retry) - Throws ConflictException if key exists
 * with different hash (conflict)
 *
 * @author Wallace Espindola
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

  private final IdempotencyRepository idempotencyRepository;

  /**
   * Check if idempotency key exists and validate request hash.
   *
   * @param key Idempotency key
   * @param requestHash SHA-256 hash of request payload
   * @return Optional containing orderId if key exists with matching hash
   * @throws ConflictException if key exists with different hash
   */
  @Transactional(readOnly = true)
  public Optional<String> checkIdempotency(String key, String requestHash) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }

    Optional<IdempotencyKeyEntity> existing = idempotencyRepository.findByKey(key);

    if (existing.isEmpty()) {
      return Optional.empty();
    }

    IdempotencyKeyEntity entity = existing.get();

    if (!entity.getRequestHash().equals(requestHash)) {
      log.warn(
          "Idempotency conflict: key={}, existing hash={}, new hash={}",
          key,
          entity.getRequestHash(),
          requestHash);
      throw new ConflictException(
          "Idempotency key '" + key + "' was already used with a different request payload");
    }

    log.debug("Idempotency hit: key={}, orderId={}", key, entity.getOrderId());
    return Optional.of(entity.getOrderId());
  }

  /** Store idempotency key with order ID and request hash. */
  @Transactional
  public void storeIdempotency(String key, String orderId, String requestHash) {
    if (key == null || key.isBlank()) {
      return;
    }

    IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
    entity.setKey(key);
    entity.setOrderId(orderId);
    entity.setRequestHash(requestHash);

    idempotencyRepository.save(entity);
    log.debug("Stored idempotency: key={}, orderId={}", key, orderId);
  }

  /** Compute SHA-256 hash of request payload. */
  public String computeRequestHash(String payload) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }
}
