package com.example.contractfirst.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.example.contractfirst.entity.IdempotencyKeyEntity;
import com.example.contractfirst.exception.ConflictException;
import com.example.contractfirst.repository.IdempotencyRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for IdempotencyService.
 *
 * @author Wallace Espindola
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

  @Mock private IdempotencyRepository idempotencyRepository;

  private IdempotencyService idempotencyService;

  @BeforeEach
  void setUp() {
    idempotencyService = new IdempotencyService(idempotencyRepository);
  }

  @Test
  void checkIdempotency_ShouldReturnEmpty_WhenKeyNotExists() {
    // Given
    String key = "key-123";
    String hash = "hash-abc";

    when(idempotencyRepository.findByKey(key)).thenReturn(Optional.empty());

    // When
    Optional<String> result = idempotencyService.checkIdempotency(key, hash);

    // Then
    assertThat(result).isEmpty();
    verify(idempotencyRepository).findByKey(key);
  }

  @Test
  void checkIdempotency_ShouldReturnOrderId_WhenKeyExistsWithSameHash() {
    // Given
    String key = "key-123";
    String hash = "hash-abc";
    String orderId = "ORD-12345";

    IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
    entity.setKey(key);
    entity.setOrderId(orderId);
    entity.setRequestHash(hash);

    when(idempotencyRepository.findByKey(key)).thenReturn(Optional.of(entity));

    // When
    Optional<String> result = idempotencyService.checkIdempotency(key, hash);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(orderId);
  }

  @Test
  void checkIdempotency_ShouldThrowConflict_WhenKeyExistsWithDifferentHash() {
    // Given
    String key = "key-123";
    String existingHash = "hash-abc";
    String newHash = "hash-xyz";

    IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
    entity.setKey(key);
    entity.setOrderId("ORD-12345");
    entity.setRequestHash(existingHash);

    when(idempotencyRepository.findByKey(key)).thenReturn(Optional.of(entity));

    // When & Then
    assertThatThrownBy(() -> idempotencyService.checkIdempotency(key, newHash))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already used with a different request payload");
  }

  @Test
  void storeIdempotency_ShouldSaveEntity() {
    // Given
    String key = "key-123";
    String orderId = "ORD-12345";
    String hash = "hash-abc";

    ArgumentCaptor<IdempotencyKeyEntity> entityCaptor =
        ArgumentCaptor.forClass(IdempotencyKeyEntity.class);

    // When
    idempotencyService.storeIdempotency(key, orderId, hash);

    // Then
    verify(idempotencyRepository).save(entityCaptor.capture());

    IdempotencyKeyEntity savedEntity = entityCaptor.getValue();
    assertThat(savedEntity.getKey()).isEqualTo(key);
    assertThat(savedEntity.getOrderId()).isEqualTo(orderId);
    assertThat(savedEntity.getRequestHash()).isEqualTo(hash);
  }

  @Test
  void computeRequestHash_ShouldReturnConsistentHash() {
    // Given
    String payload = "{\"customerId\":\"CUST-123\"}";

    // When
    String hash1 = idempotencyService.computeRequestHash(payload);
    String hash2 = idempotencyService.computeRequestHash(payload);

    // Then
    assertThat(hash1).isNotNull();
    assertThat(hash1).isNotEmpty();
    assertThat(hash1).isEqualTo(hash2); // Same input produces same hash
  }

  @Test
  void computeRequestHash_ShouldReturnDifferentHash_ForDifferentPayloads() {
    // Given
    String payload1 = "{\"customerId\":\"CUST-123\"}";
    String payload2 = "{\"customerId\":\"CUST-456\"}";

    // When
    String hash1 = idempotencyService.computeRequestHash(payload1);
    String hash2 = idempotencyService.computeRequestHash(payload2);

    // Then
    assertThat(hash1).isNotEqualTo(hash2);
  }
}
