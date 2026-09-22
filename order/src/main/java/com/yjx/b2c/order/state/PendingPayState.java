package com.yjx.b2c.order.state;

import cn.hutool.core.lang.Assert;
import com.yjx.b2c.order.bean.OrderContext;
import com.yjx.b2c.order.bean.PayResult;
import com.yjx.b2c.order.dao.mapper.OrderMapper;
import com.yjx.b2c.order.enums.OrderStatus;
import com.yjx.b2c.order.enums.PayTypeEnum;
import com.yjx.b2c.order.service.PayStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 待支付状态：可以支付，支付成功后流转到已支付
 */
@Component
public class PendingPayState implements OrderState {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private PayStrategyFactory payStrategyFactory;
    @Autowired
    private PaidState paidState;

    @Override
    public OrderStatus status() {
        return OrderStatus.PENDING_PAY;
    }

    @Override
    public OrderState pay(OrderContext context) {
        PayTypeEnum payType = context.getPayType();
        Assert.notNull(payType, "不支持的支付方式");

        // 策略模式：按支付方式选择具体支付策略
        PayResult payResult = payStrategyFactory.get(payType).pay(context.getOrder());
        Assert.isTrue(payResult.isSuccess(), "支付失败：" + payResult.getMessage());

        // 支付成功后流转订单状态：待支付 -> 已支付
        int row = orderMapper.markPaid(context.getOrder().getId(), payType.getCode(), payResult.getTradeNo());
        Assert.equals(row, 1, "订单支付状态更新失败");

        context.getOrder().setStatus(OrderStatus.PAID.getCode());
        context.getOrder().setPayType(payType.getCode());
        context.getOrder().setTradeNo(payResult.getTradeNo());
        return paidState;
    }

    @Override
    public OrderState ship(OrderContext context) {
        return reject("发货");
    }
}
