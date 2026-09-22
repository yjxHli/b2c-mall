-- ============================================
--  订单表（order.db）
--  状态：0-待支付 1-已支付 2-已发货 3-已完成 4-已取消
-- ============================================
CREATE TABLE IF NOT EXISTS tb_order
(
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no         VARCHAR(64)   NOT NULL,  -- 订单号
    shop_id          INTEGER       NOT NULL,  -- 所属店铺
    buyer_id         INTEGER,                 -- 买家
    sku_id           INTEGER,                 -- 商品id
    product_name     VARCHAR(200),            -- 商品名称快照
    quantity         INTEGER       DEFAULT 1, -- 数量
    amount           DECIMAL(10, 2) NOT NULL, -- 订单金额
    status           TINYINT       NOT NULL,  -- 见 OrderStatus
    receiver_name    VARCHAR(50),             -- 收货人
    receiver_phone   VARCHAR(20),             -- 收货电话
    receiver_address VARCHAR(500),            -- 收货地址
    pay_time         DATETIME,                -- 支付时间
    pay_type         TINYINT,                 -- 支付方式：1-支付宝 2-微信
    trade_no         VARCHAR(100),            -- 支付流水号
    ship_time        DATETIME,                -- 发货时间
    logistics_no     VARCHAR(100),            -- 物流单号
    operator_id      INTEGER,                 -- 发货操作人
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_order_no ON tb_order (order_no);
CREATE INDEX IF NOT EXISTS idx_order_status ON tb_order (status);
CREATE INDEX IF NOT EXISTS idx_order_shop_id ON tb_order (shop_id);
