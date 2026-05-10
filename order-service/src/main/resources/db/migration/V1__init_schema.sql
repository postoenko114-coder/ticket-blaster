CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        event_id BIGINT NOT NULL,
                        quantity INT,
                        created_at TIMESTAMP NOT NULL,
                        status VARCHAR(50) NOT NULL,
                        idempotency_key VARCHAR(255) NOT NULL UNIQUE
);