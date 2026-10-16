package com.yjx.b2c.shop.dao.po;

import lombok.Data;

import java.util.Date;

/**
 * 注册事件本地消息表（Outbox）：保证跨服务调用失败后可补偿、可重试。
 */
@Data
public class OutboxPO {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_RETRY = 2;
    public static final int STATUS_ABANDONED = 3;

    private Integer id;
    private String eventType;
    private Integer aggregateId;
    private String payload;
    private Integer status;
    private Integer retryCount;
    private Date nextRetryAt;
    private String lastError;
    private Date createdAt;
    private Date updatedAt;
}
