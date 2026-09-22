package com.yjx.b2c.order.state;

import com.yjx.b2c.order.bean.OrderContext;
import com.yjx.b2c.order.enums.OrderStatus;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 状态模式：订单状态接口，每个状态自己决定能做哪些操作、流转到哪个状态
 */
public interface OrderState {

    /** 当前状态对应的订单状态枚举 */
    OrderStatus status();

    /** 支付：允许则执行并返回下一个状态，否则抛出业务异常 */
    OrderState pay(OrderContext context);

    /** 发货：允许则执行并返回下一个状态，否则抛出业务异常 */
    OrderState ship(OrderContext context);

    /** 当前状态不支持该操作时的统一处理 */
    default OrderState reject(String action) {
        throw new IllegalArgumentException("订单[" + status().getDesc() + "]不允许" + action);
    }
}
