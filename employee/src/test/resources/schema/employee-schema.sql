CREATE TABLE IF NOT EXISTS tb_employee
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    shop_id         INTEGER      NOT NULL,
    username        VARCHAR(50)  NOT NULL,
    password        VARCHAR(255) NOT NULL,
    avatar_url      VARCHAR(500) DEFAULT '/avatars/default.png',
    last_login_time DATETIME,
    login_count     INTEGER      DEFAULT 0,
    status          TINYINT      DEFAULT 1,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_shop_id ON tb_employee (shop_id);
CREATE INDEX IF NOT EXISTS idx_employee_username ON tb_employee (username);

CREATE UNIQUE INDEX IF NOT EXISTS uk_employee_shop_username ON tb_employee (shop_id, username);

-- 登录审计日志（user_type 恒为 2）
CREATE TABLE IF NOT EXISTS tb_login_logs
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id        INTEGER NOT NULL,
    user_type      TINYINT      DEFAULT 2,
    login_ip       VARCHAR(50),
    login_device   VARCHAR(255),
    login_location VARCHAR(255),
    login_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    user_agent     VARCHAR(500),
    status         TINYINT      DEFAULT 1
    );

CREATE INDEX IF NOT EXISTS idx_login_logs_user_id ON tb_login_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_login_logs_login_time ON tb_login_logs (login_time);