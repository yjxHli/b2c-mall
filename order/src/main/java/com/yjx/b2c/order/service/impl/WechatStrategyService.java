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
 * @description 微信支付策略（此处模拟调用微信支付网关）
 */
@Slf4j
@Component
public class WechatStrategyService implements PayStrategyService {

    @Override
    public PayTypeEnum payType() {
        return PayTypeEnum.WECHAT;
    }

    @Override
    public PayResult pay(OrderPO order) {
        String tradeNo = "WX" + System.currentTimeMillis();
        log.info("微信支付成功: orderNo={}, amount={}, tradeNo={}", order.getOrderNo(), order.getAmount(), tradeNo);
        return PayResult.success(tradeNo);
    }
}
