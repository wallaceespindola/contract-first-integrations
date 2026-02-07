package com.example.contractfirst.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for OrderItemEntity.
 *
 * Combines orderId + sku as defined in database contract.
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
