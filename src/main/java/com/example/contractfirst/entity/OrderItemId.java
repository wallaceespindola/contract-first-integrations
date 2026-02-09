package com.example.contractfirst.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite primary key for OrderItemEntity.
 *
 * <p>Combines orderId + sku as defined in database contract.
 *
 * @author Wallace Espindola
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemId implements Serializable {
  private String orderId;
  private String sku;
}
