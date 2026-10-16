package com.yjx.b2c.shop.service;

import com.yjx.b2c.shop.dao.po.OutboxPO;

import java.util.List;

/**
 * 注册事件本地消息表服务。
 */
public interface OutboxService {

    OutboxPO enqueue(String eventType, Integer aggregateId, Object payload);

    void markSuccess(Integer id);

    void markRetry(OutboxPO po, String lastError);

    List<OutboxPO> findRetryable(int maxRetry, int limit);
}
