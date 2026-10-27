CREATE TABLE orders (
    id                 VARCHAR(36)  PRIMARY KEY,
    customer_id        VARCHAR(100) NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    total_amount_cents BIGINT       NOT NULL,
    failure_reason     VARCHAR(500),
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_orders_customer ON orders (customer_id, created_at DESC);

CREATE TABLE order_items (
    order_id         VARCHAR(36)  NOT NULL REFERENCES orders (id),
    product_id       VARCHAR(100) NOT NULL,
    quantity         INT          NOT NULL,
    unit_price_cents BIGINT       NOT NULL
);
CREATE INDEX idx_order_items_order ON order_items (order_id);

-- Append-only event store (event sourcing). Rows are never updated or deleted.
CREATE TABLE order_events (
    id          BIGSERIAL    PRIMARY KEY,
    order_id    VARCHAR(36)  NOT NULL,
    event_type  VARCHAR(80)  NOT NULL,
    payload     TEXT         NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_order_events_order ON order_events (order_id, id);

-- Idempotent consumer bookkeeping
CREATE TABLE processed_events (
    event_id     VARCHAR(36) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
