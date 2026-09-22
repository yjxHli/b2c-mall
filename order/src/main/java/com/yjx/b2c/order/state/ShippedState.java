package com.yjx.b2c.order.state;

import com.yjx.b2c.order.bean.OrderContext;
import com.yjx.b2c.order.enums.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 已发货状态：不能再支付或重复发货
 */
@Component
public class ShippedState implements OrderState {

    @Override
    public OrderStatus status() {
        return OrderStatus.SHIPPED;
    }

    @Override
    public OrderState pay(OrderContext context) {
        return reject("重复支付");
    }

    @Override
    public OrderState ship(OrderContext context) {
        return reject("重复发货");
    }
}
