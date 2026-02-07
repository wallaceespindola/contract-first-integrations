package com.example.contractfirst.mapper;

import com.example.contractfirst.dto.OrderItem;
import com.example.contractfirst.dto.OrderResponse;
import com.example.contractfirst.entity.OrderEntity;
import com.example.contractfirst.entity.OrderItemEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between entities and DTOs.
 *
 * @author Wallace Espindola
 */
@Component
public class OrderMapper {

    /**
     * Convert OrderEntity to OrderResponse DTO.
     */
    public OrderResponse toResponse(OrderEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(this::toOrderItem)
                .collect(Collectors.toList());

        return new OrderResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getStatus(),
                items,
                Instant.now() // Timestamp as per Java Developer Agent requirement
        );
    }

    /**
     * Convert OrderItem DTO to OrderItemEntity.
     */
    public OrderItemEntity toEntity(String orderId, OrderItem item) {
        return new OrderItemEntity(
                orderId,
                item.sku(),
                item.quantity()
        );
    }

    /**
     * Convert OrderItemEntity to OrderItem DTO.
     */
    public OrderItem toOrderItem(OrderItemEntity entity) {
        return new OrderItem(
                entity.getSku(),
                entity.getQuantity()
        );
    }
}
