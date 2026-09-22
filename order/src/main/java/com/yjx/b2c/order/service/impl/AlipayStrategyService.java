package com.yjx.b2c.order.service.impl;

import com.yjx.b2c.order.bean.PayResult;
import com.yjx.b2c.order.dao.po.OrderPO;
import com.yjx.b2c.order.enums.PayTypeEnum;
import com.yjx.b2c.order.service.PayStrategyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author yjx
 * @date 2026/9/17
 * @description 支付宝支付策略（此处模拟调用支付宝网关）
 */
@Slf4j
@Component
public class AlipayStrategyService implements PayStrategyService {

    @Override
    public PayTypeEnum payType() {
        return PayTypeEnum.ALIPAY;
    }

    @Override
    public PayResult pay(OrderPO order) {
        String tradeNo = "ALI" + System.currentTimeMillis();
        log.info("支付宝支付成功: orderNo={}, amount={}, tradeNo={}", order.getOrderNo(), order.getAmount(), tradeNo);
        return PayResult.success(tradeNo);
    }
}
