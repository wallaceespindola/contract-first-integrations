package com.example.contractfirst.service;

import com.acme.events.OrderCreated;
import com.acme.events.OrderItem;
import com.example.contractfirst.dto.CreateOrderRequest;
import com.example.contractfirst.dto.OrderResponse;
import com.example.contractfirst.entity.OrderEntity;
import com.example.contractfirst.entity.OrderItemEntity;
import com.example.contractfirst.exception.ResourceNotFoundException;
import com.example.contractfirst.kafka.producer.OrderEventPublisher;
import com.example.contractfirst.mapper.OrderMapper;
import com.example.contractfirst.repository.OrderItemRepository;
import com.example.contractfirst.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core business logic service for order management.
 *
 * <p>Orchestrates: - Idempotency checking (REST API pattern) - Order creation and persistence
 * (database) - Event publishing (Kafka pattern)
 *
 * @author Wallace Espindola
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final IdempotencyService idempotencyService;
  private final OrderEventPublisher eventPublisher;
  private final OrderMapper orderMapper;
  private final ObjectMapper objectMapper;

  /**
   * Create a new order with idempotency support.
   *
   * @param request Create order request
   * @return Order response DTO
   */
  @Transactional
  public OrderResponse createOrder(CreateOrderRequest request) {
    log.info("Creating order for customer: {}", request.customerId());

    // 1. Check idempotency
    String requestHash = computeRequestHash(request);
    String idempotencyKey = request.idempotencyKey();

    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      Optional<String> cachedOrderId =
          idempotencyService.checkIdempotency(idempotencyKey, requestHash);
      if (cachedOrderId.isPresent()) {
        log.info("Idempotency hit: returning cached order {}", cachedOrderId.get());
        return getOrder(cachedOrderId.get())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Cached order not found: " + cachedOrderId.get()));
      }
    }

    // 2. Generate order ID
    String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    // 3. Persist order entity
    OrderEntity orderEntity = new OrderEntity();
    orderEntity.setId(orderId);
    orderEntity.setCustomerId(request.customerId());
    orderEntity.setStatus("CREATED");
    orderEntity.setSource(null); // Will be added in future enhancements

    orderEntity = orderRepository.save(orderEntity);

    // 4. Persist order items
    List<OrderItemEntity> itemEntities =
        request.items().stream()
            .map(item -> orderMapper.toEntity(orderId, item))
            .collect(Collectors.toList());

    orderItemRepository.saveAll(itemEntities);
    orderEntity.setItems(itemEntities);

    // 5. Store idempotency key
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      idempotencyService.storeIdempotency(idempotencyKey, orderId, requestHash);
    }

    // 6. Publish Kafka event
    publishOrderCreatedEvent(orderEntity);

    log.info("Order created successfully: {}", orderId);

    // 7. Return response
    return orderMapper.toResponse(orderEntity);
  }

  /**
   * Get an existing order by ID.
   *
   * @param orderId Order ID
   * @return Optional containing order response if found
   */
  @Transactional(readOnly = true)
  public Optional<OrderResponse> getOrder(String orderId) {
    log.debug("Fetching order: {}", orderId);

    return orderRepository
        .findById(orderId)
        .map(
            entity -> {
              // Eagerly load items
              List<OrderItemEntity> items = orderItemRepository.findByOrderId(orderId);
              entity.setItems(items);
              return orderMapper.toResponse(entity);
            });
  }

  /** Publish OrderCreated event to Kafka. */
  private void publishOrderCreatedEvent(OrderEntity orderEntity) {
    try {
      // Build Avro event
      List<OrderItem> avroItems =
          orderEntity.getItems().stream()
              .map(
                  item ->
                      OrderItem.newBuilder()
                          .setSku(item.getSku())
                          .setQuantity(item.getQuantity())
                          .build())
              .collect(Collectors.toList());

      OrderCreated event =
          OrderCreated.newBuilder()
              .setEventId(UUID.randomUUID().toString())
              .setOccurredAt(Instant.now().toString())
              .setOrderId(orderEntity.getId())
              .setCustomerId(orderEntity.getCustomerId())
              .setSource(orderEntity.getSource()) // Nullable field for backward compatibility
              .setItems(avroItems)
              .build();

      eventPublisher.publishOrderCreated(event);
      log.info(
          "Published OrderCreated event: orderId={}, eventId={}",
          orderEntity.getId(),
          event.getEventId());
    } catch (Exception e) {
      log.error("Failed to publish OrderCreated event for order: {}", orderEntity.getId(), e);
      // Note: In production, use Outbox pattern to guarantee event delivery
      throw new RuntimeException("Failed to publish event", e);
    }
  }

  /** Compute request hash for idempotency checking. */
  private String computeRequestHash(CreateOrderRequest request) {
    try {
      String json = objectMapper.writeValueAsString(request);
      return idempotencyService.computeRequestHash(json);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize request", e);
    }
  }
}
