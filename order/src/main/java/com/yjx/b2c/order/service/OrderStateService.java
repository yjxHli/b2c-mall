package com.yjx.b2c.order.service;

import com.yjx.b2c.order.bean.OrderContext;
import com.yjx.b2c.order.dao.po.OrderPO;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 订单状态机服务（内部委托给状态模式的状态类）
 */
public interface OrderStateService {

    /** 支付：待支付 -> 已支付 */
    OrderPO pay(OrderContext context);

    /** 发货：已支付 -> 已发货 */
    OrderPO ship(OrderContext context);
}
