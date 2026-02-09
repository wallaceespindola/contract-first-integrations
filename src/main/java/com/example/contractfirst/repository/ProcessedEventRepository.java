package com.example.contractfirst.repository;

import com.example.contractfirst.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for ProcessedEventEntity.
 *
 * <p>Supports Kafka consumer idempotency pattern.
 *
 * @author Wallace Espindola
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {

  /** Check if an event has already been processed. */
  boolean existsByEventId(String eventId);
}
