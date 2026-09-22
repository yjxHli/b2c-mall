package com.yjx.b2c.order.web.controller;

import com.yjx.b2c.common.response.BaseResponseVO;
import com.yjx.b2c.order.bean.OrderContext;
import com.yjx.b2c.order.enums.PayTypeEnum;
import com.yjx.b2c.order.service.OrderService;
import com.yjx.b2c.order.service.OrderStateService;
import com.yjx.b2c.order.web.request.CreateOrderRequest;
import com.yjx.b2c.order.web.request.PayRequestVO;
import com.yjx.b2c.order.web.request.ShipRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yjx
 * @date 2026/9/18
 * @description
 */
@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderStateService orderStateService;

    /** 下单：创建待支付订单 */
    @PostMapping("/create")
    public BaseResponseVO<Integer> create(@RequestBody @Validated CreateOrderRequest req) {
        return new BaseResponseVO<>(orderService.createOrder(req));
    }

    /** 支付：策略模式选择支付方式，支付成功后由状态模式流转 待支付 -> 已支付 */
    @PostMapping("/pay")
    public BaseResponseVO<Integer> pay(@RequestBody @Validated PayRequestVO req) {
        OrderContext context = new OrderContext();
        context.setOrderId(req.getOrderId());
        context.setPayType(PayTypeEnum.of(req.getPayType()));

        return new BaseResponseVO<>(orderStateService.pay(context).getId());
    }

    /** 发货：状态模式流转 已支付 -> 已发货 */
    @PostMapping("/ship")
    public BaseResponseVO<Integer> ship(@RequestBody @Validated ShipRequestVO req) {
        OrderContext context = new OrderContext();
        context.setOrderId(req.getOrderId());
        context.setLogisticsNo(req.getLogisticsNo());
        context.setOperatorId(req.getOperatorId());

        return new BaseResponseVO<>(orderStateService.ship(context).getId());
    }
}
