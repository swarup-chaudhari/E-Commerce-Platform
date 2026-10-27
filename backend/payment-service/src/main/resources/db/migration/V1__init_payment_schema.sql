CREATE TABLE payments (
    id             VARCHAR(36)  PRIMARY KEY,
    order_id       VARCHAR(36)  NOT NULL UNIQUE,   -- one payment per order => idempotent saga step
    customer_id    VARCHAR(100),
    amount_cents   BIGINT       NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    failure_reason VARCHAR(500),
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    version        BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE processed_events (
    event_id     VARCHAR(36) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
