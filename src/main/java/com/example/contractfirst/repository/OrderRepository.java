package com.example.contractfirst.repository;

import com.example.contractfirst.entity.OrderEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for OrderEntity.
 *
 * @author Wallace Espindola
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {

  /** Find all orders for a specific customer. */
  List<OrderEntity> findByCustomerId(String customerId);
}
