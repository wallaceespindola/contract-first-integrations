package com.example.contractfirst.kafka.consumer;

import com.acme.events.OrderCreated;
import com.example.contractfirst.entity.ProcessedEventEntity;
import com.example.contractfirst.kafka.KafkaTopics;
import com.example.contractfirst.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka consumer for OrderCreated events.
 *
 * <p>Demonstrates idempotent event processing: - Checks eventId before processing (deduplication) -
 * Stores eventId after successful processing - Skips already-processed events (safe for
 * at-least-once delivery)
 *
 * <p>Example consumer implementation: billing system
 *
 * @author Wallace Espindola
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedListener {

  private final ProcessedEventRepository processedEventRepository;

  /**
   * Listen to orders.order-created.v1 topic.
   *
   * <p>Consumer group: billing (simulates billing system)
   */
  @KafkaListener(
      topics = KafkaTopics.ORDER_CREATED_V1,
      groupId = "billing",
      containerFactory = "kafkaListenerContainerFactory")
  @Transactional
  public void onOrderCreated(OrderCreated event, Acknowledgment acknowledgment) {
    String eventId = event.getEventId();
    String orderId = event.getOrderId();

    log.info("Received OrderCreated event: eventId={}, orderId={}", eventId, orderId);

    try {
      // 1. Check if already processed (idempotency)
      if (processedEventRepository.existsByEventId(eventId)) {
        log.info("Event already processed, skipping: eventId={}", eventId);
        acknowledgment.acknowledge();
        return;
      }

      // 2. Process event (business logic)
      processOrder(event);

      // 3. Mark event as processed
      ProcessedEventEntity processedEvent = new ProcessedEventEntity();
      processedEvent.setEventId(eventId);
      processedEvent.setEventType("OrderCreated");
      processedEventRepository.save(processedEvent);

      log.info("Successfully processed OrderCreated: orderId={}, eventId={}", orderId, eventId);

      // 4. Commit offset
      acknowledgment.acknowledge();

    } catch (Exception e) {
      log.error("Error processing OrderCreated event: eventId={}, orderId={}", eventId, orderId, e);
      // Don't acknowledge - message will be retried
      throw e;
    }
  }

  /** Business logic: process order (example: create invoice for billing). */
  private void processOrder(OrderCreated event) {
    log.info(
        "Processing order for billing: orderId={}, customerId={}, items={}",
        event.getOrderId(),
        event.getCustomerId(),
        event.getItems().size());

    // Example billing logic:
    // - Calculate total amount
    // - Create invoice
    // - Send to payment gateway
    // For demo, we just log

    log.info("Billing processed successfully for order: {}", event.getOrderId());
  }
}
