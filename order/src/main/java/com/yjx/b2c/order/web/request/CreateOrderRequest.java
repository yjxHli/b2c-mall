package com.yjx.b2c.order.web.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 下单请求
 */
@Data
public class CreateOrderRequest {
    private Integer shopId;
    private Integer buyerId;
    private Integer skuId;
    private String productName;
    private Integer quantity;
    private BigDecimal amount;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
}
