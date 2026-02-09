package com.example.contractfirst.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.contractfirst.dto.CreateOrderRequest;
import com.example.contractfirst.dto.OrderItem;
import com.example.contractfirst.dto.OrderResponse;
import com.example.contractfirst.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Controller tests for OrderController.
 *
 * <p>Uses MockMvc to test REST endpoints.
 *
 * @author Wallace Espindola
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private OrderService orderService;

  @Test
  void createOrder_ShouldReturn201_WhenValidRequest() throws Exception {
    // Given
    CreateOrderRequest request =
        new CreateOrderRequest("CUST-123", "key-123", List.of(new OrderItem("SKU-001", 2)));

    OrderResponse response =
        new OrderResponse(
            "ORD-12345",
            "CUST-123",
            "CREATED",
            List.of(new OrderItem("SKU-001", 2)),
            Instant.now());

    when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

    // When & Then
    mockMvc
        .perform(
            post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderId", is("ORD-12345")))
        .andExpect(jsonPath("$.customerId", is("CUST-123")))
        .andExpect(jsonPath("$.status", is("CREATED")))
        .andExpect(jsonPath("$.timestamp", notNullValue()));
  }

  @Test
  void createOrder_ShouldReturn400_WhenCustomerIdMissing() throws Exception {
    // Given
    CreateOrderRequest request =
        new CreateOrderRequest(
            null, // Missing customerId
            null,
            List.of(new OrderItem("SKU-001", 2)));

    // When & Then
    mockMvc
        .perform(
            post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
        .andExpect(jsonPath("$.traceId", notNullValue()));
  }

  @Test
  void createOrder_ShouldReturn400_WhenItemsEmpty() throws Exception {
    // Given
    CreateOrderRequest request =
        new CreateOrderRequest(
            "CUST-123", null, List.of() // Empty items
            );

    // When & Then
    mockMvc
        .perform(
            post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
  }

  @Test
  void getOrder_ShouldReturn200_WhenOrderExists() throws Exception {
    // Given
    String orderId = "ORD-12345";

    OrderResponse response =
        new OrderResponse(
            orderId, "CUST-123", "CREATED", List.of(new OrderItem("SKU-001", 2)), Instant.now());

    when(orderService.getOrder(orderId)).thenReturn(Optional.of(response));

    // When & Then
    mockMvc
        .perform(get("/v1/orders/{orderId}", orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderId", is(orderId)))
        .andExpect(jsonPath("$.customerId", is("CUST-123")))
        .andExpect(jsonPath("$.status", is("CREATED")))
        .andExpect(jsonPath("$.timestamp", notNullValue()));
  }

  @Test
  void getOrder_ShouldReturn404_WhenOrderNotExists() throws Exception {
    // Given
    String orderId = "ORD-99999";

    when(orderService.getOrder(orderId)).thenReturn(Optional.empty());

    // When & Then
    mockMvc
        .perform(get("/v1/orders/{orderId}", orderId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code", is("NOT_FOUND")))
        .andExpect(jsonPath("$.traceId", notNullValue()));
  }
}
