package com.example.contractfirst.controller;

import com.example.contractfirst.dto.CreateOrderRequest;
import com.example.contractfirst.dto.OrderResponse;
import com.example.contractfirst.exception.ResourceNotFoundException;
import com.example.contractfirst.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for order management.
 *
 * Implements contract: contracts/openapi/orders-api.v1.yaml
 *
 * @author Wallace Espindola
 */
@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management API with idempotency support")
public class OrderController {

    private final OrderService orderService;

    /**
     * Create a new order.
     *
     * POST /v1/orders
     */
    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order with idempotency support")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Idempotency conflict")
    })
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Received create order request for customer: {}", request.customerId());

        OrderResponse response = orderService.createOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Get an existing order by ID.
     *
     * GET /v1/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "Get an order", description = "Retrieves an order by its ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        log.info("Received get order request: {}", orderId);

        OrderResponse response = orderService.getOrder(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        return ResponseEntity.ok(response);
    }
}
