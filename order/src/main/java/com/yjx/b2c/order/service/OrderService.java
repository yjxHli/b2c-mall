package com.yjx.b2c.order.service;

import com.yjx.b2c.order.web.request.CreateOrderRequest;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 订单服务
 */
public interface OrderService {

    /** 下单：创建待支付订单 */
    Integer createOrder(CreateOrderRequest req);
}
