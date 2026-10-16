CREATE TABLE IF NOT EXISTS tb_shop
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    shop_name       VARCHAR(100) NOT NULL,
    admin_account   VARCHAR(100) NOT NULL,
    admin_password  VARCHAR(100) NOT NULL,
    logo_url        VARCHAR(500),
    status          TINYINT  DEFAULT 1,
    last_login_time DATETIME,
    login_count     INTEGER  DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS tb_messages
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    shop_id    INTEGER,
    sender_id  INTEGER,
    title      VARCHAR(200),
    content    TEXT,
    msg_type   TINYINT  DEFAULT 1,
    is_read    BOOLEAN  DEFAULT 0,
    read_time  DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_shop_admin_account ON tb_shop (admin_account);

-- 店铺名唯一：防并发重复注册
CREATE UNIQUE INDEX IF NOT EXISTS uk_shop_shop_name ON tb_shop (shop_name);

-- 管理员账号全局唯一：否则按账号登录会在多条记录中命中不确定的一条
CREATE UNIQUE INDEX IF NOT EXISTS uk_shop_admin_account ON tb_shop (admin_account);

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

CREATE INDEX IF NOT EXISTS idx_messages_shop_id ON tb_messages (shop_id);

-- 登录审计日志（shop.db 专用，user_type 恒为 1）
CREATE TABLE IF NOT EXISTS tb_login_logs
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id        INTEGER NOT NULL,
    user_type      TINYINT      DEFAULT 1,
    login_ip       VARCHAR(50),
    login_device   VARCHAR(255),
    login_location VARCHAR(255),
    login_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    user_agent     VARCHAR(500),
    status         TINYINT      DEFAULT 1
    );

CREATE INDEX IF NOT EXISTS idx_login_logs_user_id ON tb_login_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_login_logs_login_time ON tb_login_logs (login_time);