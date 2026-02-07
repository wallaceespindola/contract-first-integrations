-- V1: Create initial schema for orders, order_items, and idempotency_keys tables

-- Orders table
CREATE TABLE orders (
    id VARCHAR(32) PRIMARY KEY,
    customer_id VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_created_at ON orders(created_at);

COMMENT ON TABLE orders IS 'Stores order header information';
COMMENT ON COLUMN orders.id IS 'Unique order identifier (e.g., ORD-10001)';
COMMENT ON COLUMN orders.customer_id IS 'Customer who placed the order';
COMMENT ON COLUMN orders.status IS 'Order status (CREATED, REJECTED)';
COMMENT ON COLUMN orders.created_at IS 'Timestamp when order was created';

-- Order items table
CREATE TABLE order_items (
    order_id VARCHAR(32) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    sku VARCHAR(64) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (order_id, sku)
);

CREATE INDEX idx_order_items_sku ON order_items(sku);

COMMENT ON TABLE order_items IS 'Stores individual items within an order';
COMMENT ON COLUMN order_items.order_id IS 'Foreign key to orders table';
COMMENT ON COLUMN order_items.sku IS 'Stock Keeping Unit - product identifier';
COMMENT ON COLUMN order_items.quantity IS 'Number of units ordered (must be positive)';

-- Idempotency keys table for REST API idempotency
CREATE TABLE idempotency_keys (
    key VARCHAR(255) PRIMARY KEY,
    order_id VARCHAR(32) NOT NULL REFERENCES orders(id),
    request_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_idempotency_keys_order_id ON idempotency_keys(order_id);
CREATE INDEX idx_idempotency_keys_created_at ON idempotency_keys(created_at);

COMMENT ON TABLE idempotency_keys IS 'Tracks idempotency keys for safe request retries';
COMMENT ON COLUMN idempotency_keys.key IS 'Client-provided idempotency key (UUID)';
COMMENT ON COLUMN idempotency_keys.order_id IS 'The order created with this idempotency key';
COMMENT ON COLUMN idempotency_keys.request_hash IS 'SHA-256 hash of request payload for conflict detection';
COMMENT ON COLUMN idempotency_keys.created_at IS 'Timestamp when key was first used';

-- Processed events table for Kafka consumer idempotency
CREATE TABLE processed_events (
    event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_processed_events_type ON processed_events(event_type);
CREATE INDEX idx_processed_events_processed_at ON processed_events(processed_at);

COMMENT ON TABLE processed_events IS 'Tracks processed Kafka events for consumer idempotency';
COMMENT ON COLUMN processed_events.event_id IS 'Unique event identifier from Kafka message';
COMMENT ON COLUMN processed_events.event_type IS 'Type of event (e.g., OrderCreated)';
COMMENT ON COLUMN processed_events.processed_at IS 'Timestamp when event was processed';
