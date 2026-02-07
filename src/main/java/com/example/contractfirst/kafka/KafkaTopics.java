package com.example.contractfirst.kafka;

/**
 * Kafka topic constants.
 *
 * Maps to contract: contracts/events/topics.md
 *
 * @author Wallace Espindola
 */
public final class KafkaTopics {

    // Main topics
    public static final String ORDER_CREATED_V1 = "orders.order-created.v1";

    // Dead Letter Queues
    public static final String ORDER_CREATED_V1_DLQ = "orders.order-created.v1.dlq";

    private KafkaTopics() {
        // Utility class
    }
}
