package com.example.contractfirst.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for processed_events table.
 *
 * <p>Stores processed Kafka event IDs for consumer idempotency. Maps to contract:
 * contracts/db/flyway/V1__create_orders.sql
 *
 * @author Wallace Espindola
 */
@Entity
@Table(name = "processed_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEventEntity {

  @Id
  @Column(name = "event_id", length = 255)
  private String eventId;

  @Column(name = "event_type", length = 64, nullable = false)
  private String eventType;

  @Column(name = "processed_at", nullable = false)
  private LocalDateTime processedAt;

  @PrePersist
  protected void onCreate() {
    if (processedAt == null) {
      processedAt = LocalDateTime.now();
    }
  }
}
