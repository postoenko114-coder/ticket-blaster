CREATE TABLE notification_deliveries (
    id BIGSERIAL PRIMARY KEY,
    notification_event_id VARCHAR(120) NOT NULL UNIQUE,
    order_id VARCHAR(120) NOT NULL,
    ticket_event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    received_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_notification_deliveries_status_updated_at
    ON notification_deliveries (status, updated_at DESC);
