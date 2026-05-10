CREATE TABLE outbox_events (
                               id BIGSERIAL PRIMARY KEY,
                               aggregate_id VARCHAR(255) NOT NULL,
                               payload TEXT NOT NULL,
                               created_at TIMESTAMP NOT NULL,
                               processed BOOLEAN DEFAULT FALSE
);