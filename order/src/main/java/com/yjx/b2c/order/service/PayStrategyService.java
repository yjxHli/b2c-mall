package com.yjx.b2c.order.service;

import com.yjx.b2c.order.bean.PayResult;
import com.yjx.b2c.order.dao.po.OrderPO;
import com.yjx.b2c.order.enums.PayTypeEnum;

/**
 * @author yjx
 * @date 2026/9/17
 * @description 策略模式：支付策略
 */
public interface PayStrategyService {

    /** 支持的支付方式 */
    PayTypeEnum payType();

    /** 执行支付 */
    PayResult pay(OrderPO order);
}
