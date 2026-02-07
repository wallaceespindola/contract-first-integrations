package com.example.contractfirst.service;

import com.acme.events.OrderCreated;
import com.example.contractfirst.dto.CreateOrderRequest;
import com.example.contractfirst.dto.OrderItem;
import com.example.contractfirst.dto.OrderResponse;
import com.example.contractfirst.entity.OrderEntity;
import com.example.contractfirst.entity.OrderItemEntity;
import com.example.contractfirst.exception.ResourceNotFoundException;
import com.example.contractfirst.kafka.producer.OrderEventPublisher;
import com.example.contractfirst.mapper.OrderMapper;
import com.example.contractfirst.repository.OrderItemRepository;
import com.example.contractfirst.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService.
 *
 * Tests business logic with mocked dependencies.
 *
 * @author Wallace Espindola
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private OrderEventPublisher eventPublisher;

    @Mock
    private OrderMapper orderMapper;

    private OrderService orderService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        orderService = new OrderService(
                orderRepository,
                orderItemRepository,
                idempotencyService,
                eventPublisher,
                orderMapper,
                objectMapper
        );
    }

    @Test
    void createOrder_ShouldCreateNewOrder_WhenNoIdempotencyKey() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
                "CUST-123",
                null, // No idempotency key
                List.of(new com.example.contractfirst.dto.OrderItem("SKU-001", 2))
        );

        OrderEntity savedEntity = new OrderEntity();
        savedEntity.setId("ORD-12345");
        savedEntity.setCustomerId("CUST-123");
        savedEntity.setStatus("CREATED");

        OrderItemEntity itemEntity = new OrderItemEntity();
        itemEntity.setOrderId("ORD-12345");
        itemEntity.setSku("SKU-001");
        itemEntity.setQuantity(2);

        OrderResponse expectedResponse = new OrderResponse(
                "ORD-12345",
                "CUST-123",
                "CREATED",
                List.of(new com.example.contractfirst.dto.OrderItem("SKU-001", 2)),
                java.time.Instant.now()
        );

        when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedEntity);
        when(orderMapper.toEntity(anyString(), any(com.example.contractfirst.dto.OrderItem.class))).thenReturn(itemEntity);
        when(orderItemRepository.saveAll(anyList())).thenReturn(List.of(itemEntity));
        when(orderMapper.toResponse(any(OrderEntity.class))).thenReturn(expectedResponse);

        // When
        OrderResponse result = orderService.createOrder(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo("ORD-12345");
        assertThat(result.customerId()).isEqualTo("CUST-123");
        assertThat(result.status()).isEqualTo("CREATED");

        verify(orderRepository).save(any(OrderEntity.class));
        verify(orderItemRepository).saveAll(anyList());
        verify(eventPublisher).publishOrderCreated(any(OrderCreated.class));
        verify(idempotencyService, never()).checkIdempotency(anyString(), anyString());
    }

    @Test
    void createOrder_ShouldReturnCachedOrder_WhenIdempotencyKeyExists() {
        // Given
        String idempotencyKey = "key-123";
        CreateOrderRequest request = new CreateOrderRequest(
                "CUST-123",
                idempotencyKey,
                List.of(new com.example.contractfirst.dto.OrderItem("SKU-001", 2))
        );

        String cachedOrderId = "ORD-99999";

        OrderEntity cachedEntity = new OrderEntity();
        cachedEntity.setId(cachedOrderId);
        cachedEntity.setCustomerId("CUST-123");
        cachedEntity.setStatus("CREATED");

        OrderItemEntity itemEntity = new OrderItemEntity();
        itemEntity.setOrderId(cachedOrderId);
        itemEntity.setSku("SKU-001");
        itemEntity.setQuantity(2);

        OrderResponse cachedResponse = new OrderResponse(
                cachedOrderId,
                "CUST-123",
                "CREATED",
                List.of(new com.example.contractfirst.dto.OrderItem("SKU-001", 2)),
                java.time.Instant.now()
        );

        when(idempotencyService.checkIdempotency(eq(idempotencyKey), any()))
                .thenReturn(Optional.of(cachedOrderId));
        when(orderRepository.findById(cachedOrderId)).thenReturn(Optional.of(cachedEntity));
        when(orderItemRepository.findByOrderId(cachedOrderId)).thenReturn(List.of(itemEntity));
        when(orderMapper.toResponse(any(OrderEntity.class))).thenReturn(cachedResponse);

        // When
        OrderResponse result = orderService.createOrder(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(cachedOrderId);

        verify(idempotencyService).checkIdempotency(eq(idempotencyKey), any());
        verify(orderRepository, never()).save(any(OrderEntity.class));
        verify(eventPublisher, never()).publishOrderCreated(any(OrderCreated.class));
    }

    @Test
    void getOrder_ShouldReturnOrder_WhenExists() {
        // Given
        String orderId = "ORD-12345";

        OrderEntity entity = new OrderEntity();
        entity.setId(orderId);
        entity.setCustomerId("CUST-123");
        entity.setStatus("CREATED");

        OrderResponse expectedResponse = new OrderResponse(
                orderId,
                "CUST-123",
                "CREATED",
                List.of(),
                java.time.Instant.now()
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(entity));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(orderMapper.toResponse(any(OrderEntity.class))).thenReturn(expectedResponse);

        // When
        Optional<OrderResponse> result = orderService.getOrder(orderId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().orderId()).isEqualTo(orderId);

        verify(orderRepository).findById(orderId);
        verify(orderItemRepository).findByOrderId(orderId);
    }

    @Test
    void getOrder_ShouldReturnEmpty_WhenNotExists() {
        // Given
        String orderId = "ORD-99999";

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When
        Optional<OrderResponse> result = orderService.getOrder(orderId);

        // Then
        assertThat(result).isEmpty();

        verify(orderRepository).findById(orderId);
        verify(orderItemRepository, never()).findByOrderId(anyString());
    }

    @Test
    void createOrder_ShouldPublishKafkaEvent_WithCorrectData() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
                "CUST-123",
                null,
                List.of(new com.example.contractfirst.dto.OrderItem("SKU-001", 2))
        );

        OrderEntity savedEntity = new OrderEntity();
        savedEntity.setId("ORD-12345");
        savedEntity.setCustomerId("CUST-123");
        savedEntity.setStatus("CREATED");

        OrderItemEntity itemEntity = new OrderItemEntity();
        itemEntity.setOrderId("ORD-12345");
        itemEntity.setSku("SKU-001");
        itemEntity.setQuantity(2);

        when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedEntity);
        when(orderMapper.toEntity(anyString(), any(com.example.contractfirst.dto.OrderItem.class))).thenReturn(itemEntity);
        when(orderItemRepository.saveAll(anyList())).thenReturn(List.of(itemEntity));
        when(orderMapper.toResponse(any(OrderEntity.class))).thenReturn(
                new OrderResponse("ORD-12345", "CUST-123", "CREATED", List.of(), java.time.Instant.now())
        );

        ArgumentCaptor<OrderCreated> eventCaptor = ArgumentCaptor.forClass(OrderCreated.class);

        // When
        orderService.createOrder(request);

        // Then
        verify(eventPublisher).publishOrderCreated(eventCaptor.capture());

        OrderCreated capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOrderId()).isEqualTo("ORD-12345");
        assertThat(capturedEvent.getCustomerId()).isEqualTo("CUST-123");
        assertThat(capturedEvent.getEventId()).isNotNull();
        assertThat(capturedEvent.getOccurredAt()).isNotNull();
    }
}
