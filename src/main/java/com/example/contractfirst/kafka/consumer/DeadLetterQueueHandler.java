package com.example.contractfirst.kafka.consumer;

import com.acme.events.DeadLetterEnvelope;
import com.example.contractfirst.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Handler for failed messages - sends to Dead Letter Queue.
 *
 * Wraps failed messages in DeadLetterEnvelope with debugging information:
 * - Original topic, partition, offset
 * - Consumer group
 * - Error details
 * - Base64-encoded payload for replay
 *
 * @author Wallace Espindola
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueHandler {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Send failed message to DLQ.
     *
     * @param record Original Kafka record
     * @param consumerGroup Consumer group that failed to process
     * @param exception Exception that caused the failure
     */
    public void sendToDeadLetterQueue(ConsumerRecord<String, ?> record, String consumerGroup, Exception exception) {
        try {
            String payloadBase64 = encodePayload(record.value());

            DeadLetterEnvelope envelope = DeadLetterEnvelope.newBuilder()
                    .setOriginalTopic(record.topic())
                    .setPartition(record.partition())
                    .setOffset(record.offset())
                    .setConsumerGroup(consumerGroup)
                    .setErrorClass(exception.getClass().getName())
                    .setErrorMessage(exception.getMessage() != null ? exception.getMessage() : "No error message")
                    .setFailedAt(Instant.now().toString())
                    .setPayloadBase64(payloadBase64)
                    .build();

            String dlqTopic = determineDlqTopic(record.topic());
            String key = record.key();

            kafkaTemplate.send(dlqTopic, key, envelope);

            log.warn("Sent message to DLQ: topic={}, dlqTopic={}, key={}, error={}",
                    record.topic(), dlqTopic, key, exception.getMessage());

        } catch (Exception e) {
            log.error("Failed to send message to DLQ: topic={}, key={}, originalError={}, dlqError={}",
                    record.topic(), record.key(), exception.getMessage(), e.getMessage(), e);
        }
    }

    /**
     * Determine DLQ topic name based on original topic.
     */
    private String determineDlqTopic(String originalTopic) {
        if (KafkaTopics.ORDER_CREATED_V1.equals(originalTopic)) {
            return KafkaTopics.ORDER_CREATED_V1_DLQ;
        }
        return originalTopic + ".dlq";
    }

    /**
     * Encode message payload as Base64 for storage in DLQ.
     */
    private String encodePayload(Object payload) {
        try {
            String payloadString = payload.toString();
            return Base64.getEncoder().encodeToString(payloadString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to encode payload", e);
            return "ERROR_ENCODING_PAYLOAD";
        }
    }
}
