-- ============================================
--  商品类目
-- ============================================
CREATE TABLE IF NOT EXISTS tb_category
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id     INTEGER      DEFAULT 0,  -- 父类目，0为一级类目
    category_name VARCHAR(100) NOT NULL,   -- 类目名称
    level         TINYINT      DEFAULT 1,  -- 层级：1-一级 2-二级
    status        TINYINT      DEFAULT 1,  -- 状态：1-启用 0-禁用
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
--  商品（SKU）
-- ============================================
CREATE TABLE IF NOT EXISTS tb_sku
(
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    shop_id          INTEGER        NOT NULL,  -- 所属店铺
    category_id      INTEGER        NOT NULL,  -- 类目
    product_name     VARCHAR(200)   NOT NULL,  -- 商品名称
    product_desc     TEXT,                     -- 商品描述
    product_type     TINYINT        NOT NULL,  -- 1-实物 2-虚拟
    price            DECIMAL(10, 2) NOT NULL,  -- 单价
    stock            INTEGER        DEFAULT 0, -- 库存（虚拟商品恒为0，表示不限）
    delivery_content VARCHAR(1000),            -- 虚拟商品交付内容/链接
    cover_url        VARCHAR(500),             -- 封面图
    status           TINYINT        DEFAULT 0, -- 0-草稿 1-上架 2-下架
    operator_id      INTEGER,                  -- 操作员工
    created_at       DATETIME       DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME       DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sku_shop_id ON tb_sku (shop_id);
CREATE INDEX IF NOT EXISTS idx_sku_category_id ON tb_sku (category_id);

-- ============================================
--  商品操作日志
-- ============================================
CREATE TABLE IF NOT EXISTS tb_sku_log
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    sku_id      INTEGER NOT NULL, -- 商品id
    shop_id     INTEGER,          -- 店铺id
    operator_id INTEGER,          -- 操作员工
    action      VARCHAR(50),      -- 操作：CREATE_PRODUCT / ON_SHELF ...
    detail      TEXT,             -- 详情
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sku_log_sku_id ON tb_sku_log (sku_id);

-- ============================================
--  基础类目数据
-- ============================================
INSERT OR IGNORE INTO tb_category (id, parent_id, category_name, level, status) VALUES (1, 0, '服装', 1, 1);
INSERT OR IGNORE INTO tb_category (id, parent_id, category_name, level, status) VALUES (2, 1, '男装', 2, 1);
INSERT OR IGNORE INTO tb_category (id, parent_id, category_name, level, status) VALUES (3, 0, '数码', 1, 1);
INSERT OR IGNORE INTO tb_category (id, parent_id, category_name, level, status) VALUES (4, 3, '手机', 2, 1);
INSERT OR IGNORE INTO tb_category (id, parent_id, category_name, level, status) VALUES (5, 0, '虚拟卡券', 1, 1);
INSERT OR IGNORE INTO tb_category (id, parent_id, category_name, level, status) VALUES (6, 5, '视频会员', 2, 1);
