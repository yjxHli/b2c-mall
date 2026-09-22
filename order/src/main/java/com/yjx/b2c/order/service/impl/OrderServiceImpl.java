package com.yjx.b2c.order.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import com.yjx.b2c.order.dao.mapper.OrderMapper;
import com.yjx.b2c.order.dao.po.OrderPO;
import com.yjx.b2c.order.enums.OrderStatus;
import com.yjx.b2c.order.service.OrderService;
import com.yjx.b2c.order.web.request.CreateOrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author yjx
 * @date 2026/9/18
 * @description
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;

    @Override
    public Integer createOrder(CreateOrderRequest req) {
        Assert.notNull(req.getShopId(), "店铺不能为空");
        Assert.notNull(req.getQuantity(), "购买数量不能为空");
        Assert.isTrue(req.getQuantity() > 0, "购买数量必须大于0");
        Assert.notNull(req.getAmount(), "订单金额不能为空");
        Assert.isTrue(req.getAmount().compareTo(BigDecimal.ZERO) > 0, "订单金额必须大于0");
        Assert.notBlank(req.getReceiverName(), "收货人不能为空");
        Assert.notBlank(req.getReceiverPhone(), "收货电话不能为空");
        Assert.notBlank(req.getReceiverAddress(), "收货地址不能为空");

        OrderPO order = new OrderPO();
        order.setOrderNo(generateOrderNo());
        order.setShopId(req.getShopId());
        order.setBuyerId(req.getBuyerId());
        order.setSkuId(req.getSkuId());
        order.setProductName(req.getProductName());
        order.setQuantity(req.getQuantity());
        order.setAmount(req.getAmount());
        order.setReceiverName(req.getReceiverName());
        order.setReceiverPhone(req.getReceiverPhone());
        order.setReceiverAddress(req.getReceiverAddress());
        // 新订单初始状态：待支付
        order.setStatus(OrderStatus.PENDING_PAY.getCode());

        Integer row = orderMapper.save(order);
        Assert.equals(row, 1, "下单失败");
        log.info("下单成功: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
        return order.getId();
    }

    private String generateOrderNo() {
        return "NO" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(6);
    }
}
