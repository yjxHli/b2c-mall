CREATE TABLE IF NOT EXISTS tb_register_event_outbox
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    event_type    VARCHAR(64) NOT NULL,
    aggregate_id  INTEGER     NOT NULL,
    payload       TEXT        NOT NULL,
    status        TINYINT     DEFAULT 0,
    retry_count   INTEGER     DEFAULT 0,
    next_retry_at DATETIME    DEFAULT CURRENT_TIMESTAMP,
    last_error    VARCHAR(500),
    created_at    DATETIME    DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_retry
    ON tb_register_event_outbox (status, next_retry_at);
