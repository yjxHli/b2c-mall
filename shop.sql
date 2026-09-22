-- ============================================
--  店铺表
-- ============================================
CREATE TABLE IF NOT EXISTS tb_shop
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    shop_name       VARCHAR(100) NOT NULL, -- 店铺名
    admin_account   VARCHAR(100) NOT NULL, -- 管理员
    admin_password  VARCHAR(100) NOT NULL, -- 管理员密码
    logo_url        VARCHAR(500),          -- 店铺logo
    status          TINYINT  DEFAULT 1,    -- 状态：1-正常 0-禁用
    last_login_time DATETIME,              -- 最后登录时间
    login_count     INTEGER  DEFAULT 0,    -- 累计登录次数
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_shop_admin_account ON tb_shop (admin_account);


-- ============================================
-- 站内信表
-- ============================================
CREATE TABLE IF NOT EXISTS tb_messages
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    shop_id    INTEGER,
    sender_id  INTEGER,            -- 发送者（系统消息可为NULL）
    title      VARCHAR(200),       -- 消息标题
    content    TEXT,               -- 消息内容
    msg_type   TINYINT  DEFAULT 1, -- 1-系统通知 2-个人消息 3-公告
    is_read    BOOLEAN  DEFAULT 0, -- 是否已读
    read_time  DATETIME,           -- 阅读时间
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_messages_shop_id ON tb_messages (shop_id);


-- ============================================
--  登录日志表（安全审计，shop.db 专用，user_type 恒为 1）
-- ============================================
CREATE TABLE IF NOT EXISTS tb_login_logs
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id        INTEGER NOT NULL,                      -- 店铺id
    user_type      TINYINT      DEFAULT 1,                -- 1-店铺
    login_ip       VARCHAR(50),                           -- 登录IP
    login_device   VARCHAR(255),                          -- 登录设备
    login_location VARCHAR(255),                          -- 登录地点
    login_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,-- 登录时间
    user_agent     VARCHAR(500),                          -- 浏览器UA
    status         TINYINT      DEFAULT 1                 -- 1-成功 0-失败
);

CREATE INDEX IF NOT EXISTS idx_login_logs_user_id ON tb_login_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_login_logs_login_time ON tb_login_logs (login_time);
