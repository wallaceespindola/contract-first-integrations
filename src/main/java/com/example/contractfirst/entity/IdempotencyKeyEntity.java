package com.example.contractfirst.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity for idempotency_keys table.
 *
 * Stores idempotency keys for REST API request deduplication.
 * Maps to contract: contracts/db/flyway/V1__create_orders.sql
 *
 * @author Wallace Espindola
 */
@Entity
@Table(name = "idempotency_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKeyEntity {

    @Id
    @Column(name = "key", length = 255)
    private String key;

    @Column(name = "order_id", length = 32, nullable = false)
    private String orderId;

    @Column(name = "request_hash", length = 64, nullable = false)
    private String requestHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
