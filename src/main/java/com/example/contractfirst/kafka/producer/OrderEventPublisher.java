package com.example.contractfirst.kafka.producer;

import com.acme.events.OrderCreated;
import com.example.contractfirst.kafka.KafkaTopics;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for OrderCreated events.
 *
 * <p>Publishes events to: orders.order-created.v1 Uses Avro serialization with Schema Registry.
 *
 * @author Wallace Espindola
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  /**
   * Publish OrderCreated event to Kafka.
   *
   * <p>Key: orderId (for partition affinity) Value: OrderCreated Avro event
   */
  public void publishOrderCreated(OrderCreated event) {
    String key = event.getOrderId();

    log.debug("Publishing OrderCreated event: orderId={}, eventId={}", key, event.getEventId());

    CompletableFuture<SendResult<String, Object>> future =
        kafkaTemplate.send(KafkaTopics.ORDER_CREATED_V1, key, event);

    future.whenComplete(
        (result, ex) -> {
          if (ex == null) {
            log.info(
                "Successfully published OrderCreated: orderId={}, partition={}, offset={}",
                key,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          } else {
            log.error("Failed to publish OrderCreated: orderId={}", key, ex);
          }
        });
  }
}
