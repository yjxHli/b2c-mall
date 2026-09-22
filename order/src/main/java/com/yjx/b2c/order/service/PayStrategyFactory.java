package com.yjx.b2c.order.service;

import cn.hutool.core.lang.Assert;
import com.yjx.b2c.order.enums.PayTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 支付策略工厂：按支付方式选择支付策略
 */
@Component
public class PayStrategyFactory {
    private final Map<PayTypeEnum, PayStrategyService> strategies = new EnumMap<>(PayTypeEnum.class);

    @Autowired
    public PayStrategyFactory(List<PayStrategyService> strategyList) {
        for (PayStrategyService strategy : strategyList) {
            strategies.put(strategy.payType(), strategy);
        }
    }

    public PayStrategyService get(PayTypeEnum payType) {
        PayStrategyService strategy = payType == null ? null : strategies.get(payType);
        Assert.notNull(strategy, "不支持的支付方式");
        return strategy;
    }
}
