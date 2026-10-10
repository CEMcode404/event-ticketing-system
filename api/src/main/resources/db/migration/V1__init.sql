CREATE TABLE events (
    id             BINARY(16)   NOT NULL,
    name           VARCHAR(255) NOT NULL,
    venue          VARCHAR(255) NOT NULL,
    description    TEXT,
    sale_opens_at  DATETIME(6)  NOT NULL,
    starts_at      DATETIME(6)  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_events_status_starts_at (status, starts_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE orders (
    id                 BINARY(16)   NOT NULL,
    event_id           BINARY(16)   NOT NULL,
    section            VARCHAR(255) NOT NULL,
    hold_token         VARCHAR(255) NOT NULL,
    admission_token    VARCHAR(255) NOT NULL,
    total_cents        INT          NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    stripe_session_id  VARCHAR(255),
    customer_email     VARCHAR(255),
    created_at         DATETIME(6)  NOT NULL,
    completed_at       DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_stripe_session_id UNIQUE (stripe_session_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE order_seats (
    order_id  BINARY(16)   NOT NULL,
    seat_id   VARCHAR(255) NOT NULL,
    CONSTRAINT fk_order_seats_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;