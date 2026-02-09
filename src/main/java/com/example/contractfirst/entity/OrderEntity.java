package com.example.contractfirst.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for orders table.
 *
 * <p>Maps to contract: contracts/db/flyway/V1__create_orders.sql
 *
 * @author Wallace Espindola
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

  @Id
  @Column(name = "id", length = 32)
  private String id;

  @Column(name = "customer_id", length = 32, nullable = false)
  private String customerId;

  @Column(name = "status", length = 16, nullable = false)
  private String status;

  @Column(name = "source", length = 32)
  private String source; // Added in V2 migration (backward compatible)

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @OneToMany(
      mappedBy = "orderId",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<OrderItemEntity> items = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
