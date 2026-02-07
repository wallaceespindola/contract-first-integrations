package com.example.contractfirst.repository;

import com.example.contractfirst.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for IdempotencyKeyEntity.
 *
 * Supports REST API idempotency pattern.
 *
 * @author Wallace Espindola
 */
@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyKeyEntity, String> {

    /**
     * Find idempotency record by key.
     */
    Optional<IdempotencyKeyEntity> findByKey(String key);

    /**
     * Check if an idempotency key exists.
     */
    boolean existsByKey(String key);
}
