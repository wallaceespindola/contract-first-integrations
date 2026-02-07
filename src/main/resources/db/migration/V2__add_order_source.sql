-- V2: Add 'source' column to orders table
-- Demonstrates expand/migrate/contract pattern for backward-compatible schema evolution

-- EXPAND: Add nullable column with default
ALTER TABLE orders ADD COLUMN source VARCHAR(32);

COMMENT ON COLUMN orders.source IS 'Source of the order (WEB, MOBILE, API, etc.) - Added in V2';

-- MIGRATE: Backfill existing rows with a default value (optional, depends on business requirements)
-- In this case, we set existing orders to 'UNKNOWN' source
UPDATE orders SET source = 'UNKNOWN' WHERE source IS NULL;

-- CONTRACT: Later, once all systems produce 'source', you can enforce NOT NULL
-- This would be done in a future migration (V3) after confirming all producers send the field:
-- ALTER TABLE orders ALTER COLUMN source SET NOT NULL;

-- For now, we leave it nullable to maintain backward compatibility with existing code
