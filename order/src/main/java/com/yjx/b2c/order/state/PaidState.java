package com.yjx.b2c.order.state;

import cn.hutool.core.lang.Assert;
import com.yjx.b2c.order.bean.OrderContext;
import com.yjx.b2c.order.dao.mapper.OrderMapper;
import com.yjx.b2c.order.enums.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 已支付状态：可以发货，发货后流转到已发货
 */
@Component
public class PaidState implements OrderState {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ShippedState shippedState;

    @Override
    public OrderStatus status() {
        return OrderStatus.PAID;
    }

    @Override
    public OrderState pay(OrderContext context) {
        return reject("重复支付");
    }

    @Override
    public OrderState ship(OrderContext context) {
        Assert.notBlank(context.getLogisticsNo(), "物流单号不能为空");

        int row = orderMapper.ship(context.getOrder().getId(), context.getLogisticsNo(), context.getOperatorId());
        Assert.equals(row, 1, "订单发货失败");

        context.getOrder().setStatus(OrderStatus.SHIPPED.getCode());
        context.getOrder().setLogisticsNo(context.getLogisticsNo());
        context.getOrder().setOperatorId(context.getOperatorId());
        return shippedState;
    }
}
