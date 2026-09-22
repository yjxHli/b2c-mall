package com.yjx.b2c.order.service.impl;

import cn.hutool.core.lang.Assert;
import com.yjx.b2c.order.bean.OrderContext;
import com.yjx.b2c.order.dao.mapper.OrderMapper;
import com.yjx.b2c.order.dao.po.OrderPO;
import com.yjx.b2c.order.enums.OrderStatus;
import com.yjx.b2c.order.service.OrderStateService;
import com.yjx.b2c.order.state.OrderState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 加载订单 -> 找到当前状态对象 -> 由状态对象决定能否操作与流转
 */
@Slf4j
@Service
public class OrderStateServiceImpl implements OrderStateService {
    private final Map<OrderStatus, OrderState> stateMap = new EnumMap<>(OrderStatus.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    public OrderStateServiceImpl(List<OrderState> stateList) {
        for (OrderState state : stateList) {
            stateMap.put(state.status(), state);
        }
    }

    @Override
    public OrderPO pay(OrderContext context) {
        return transfer(context, true);
    }

    @Override
    public OrderPO ship(OrderContext context) {
        return transfer(context, false);
    }

    private OrderPO transfer(OrderContext context, boolean payAction) {
        Assert.notNull(context, "流转上下文不能为空");
        Assert.notNull(context.getOrderId(), "订单不能为空");

        OrderPO order = orderMapper.findById(context.getOrderId());
        Assert.notNull(order, "订单不存在");
        context.setOrder(order);

        OrderStatus current = OrderStatus.of(order.getStatus());
        OrderState state = current == null ? null : stateMap.get(current);
        Assert.notNull(state, "订单状态异常");

        if (payAction) {
            state.pay(context);
        } else {
            state.ship(context);
        }
        log.info("订单状态流转成功: orderId={}, 状态={}", order.getId(), OrderStatus.of(order.getStatus()).getDesc());
        return order;
    }
}
