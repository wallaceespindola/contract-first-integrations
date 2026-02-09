package com.example.contractfirst.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for order_items table.
 *
 * <p>Uses composite key (orderId + sku) as defined in database contract. Maps to contract:
 * contracts/db/flyway/V1__create_orders.sql
 *
 * @author Wallace Espindola
 */
@Entity
@Table(name = "order_items")
@IdClass(OrderItemId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemEntity {

  @Id
  @Column(name = "order_id", length = 32, nullable = false)
  private String orderId;

  @Id
  @Column(name = "sku", length = 64, nullable = false)
  private String sku;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;
}
