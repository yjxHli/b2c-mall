package com.yjx.b2c.shop.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yjx.b2c.shop.dao.mapper.OutboxMapper;
import com.yjx.b2c.shop.dao.po.OutboxPO;
import com.yjx.b2c.shop.service.OutboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutboxServiceImpl implements OutboxService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private OutboxMapper outboxMapper;

    @Override
    public OutboxPO enqueue(String eventType, Integer aggregateId, Object payload) {
        OutboxPO po = new OutboxPO();
        po.setEventType(eventType);
        po.setAggregateId(aggregateId);
        po.setStatus(OutboxPO.STATUS_PENDING);
        try {
            po.setPayload(OBJECT_MAPPER.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("事件负载序列化失败", e);
        }
        outboxMapper.insert(po);
        return po;
    }

    @Override
    public void markSuccess(Integer id) {
        outboxMapper.markSuccess(id);
    }

    @Override
    public void markRetry(OutboxPO po, String lastError) {
        outboxMapper.markRetry(po.getId(), lastError);
    }

    @Override
    public List<OutboxPO> findRetryable(int maxRetry, int limit) {
        return outboxMapper.findRetryable(maxRetry, limit);
    }
}
