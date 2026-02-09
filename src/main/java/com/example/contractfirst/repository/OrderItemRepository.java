package com.example.contractfirst.repository;

import com.example.contractfirst.entity.OrderItemEntity;
import com.example.contractfirst.entity.OrderItemId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for OrderItemEntity.
 *
 * @author Wallace Espindola
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, OrderItemId> {

  /** Find all items for a specific order. */
  List<OrderItemEntity> findByOrderId(String orderId);
}
