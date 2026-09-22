package com.yjx.b2c.order.state;

import com.yjx.b2c.order.bean.OrderContext;
import com.yjx.b2c.order.enums.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 已取消状态：终态，不允许再支付或发货
 */
@Component
public class CancelledState implements OrderState {

    @Override
    public OrderStatus status() {
        return OrderStatus.CANCELLED;
    }

    @Override
    public OrderState pay(OrderContext context) {
        return reject("支付");
    }

    @Override
    public OrderState ship(OrderContext context) {
        return reject("发货");
    }
}
