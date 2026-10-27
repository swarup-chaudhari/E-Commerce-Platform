CREATE TABLE stock_items (
    product_id         VARCHAR(100) PRIMARY KEY,
    available_quantity INT    NOT NULL CHECK (available_quantity >= 0),
    version            BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE reservations (
    order_id   VARCHAR(36) PRIMARY KEY,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE reservation_items (
    order_id   VARCHAR(36)  NOT NULL REFERENCES reservations (order_id),
    product_id VARCHAR(100) NOT NULL,
    quantity   INT          NOT NULL
);
CREATE INDEX idx_reservation_items_order ON reservation_items (order_id);

CREATE TABLE processed_events (
    event_id     VARCHAR(36) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
