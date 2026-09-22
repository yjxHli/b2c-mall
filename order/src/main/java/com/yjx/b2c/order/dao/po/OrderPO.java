package com.yjx.b2c.order.dao.po;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 订单
 */
@Data
public class OrderPO {
    private Integer id;
    private String orderNo;
    private Integer shopId;
    private Integer buyerId;
    private Integer skuId;
    private String productName;
    private Integer quantity;
    private BigDecimal amount;
    /** 见 OrderStatus */
    private Integer status;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private Date payTime;
    /** 支付方式：1-支付宝 2-微信 */
    private Integer payType;
    /** 支付流水号 */
    private String tradeNo;
    private Date shipTime;
    private String logisticsNo;
    private Integer operatorId;
    private Date createdAt;
    private Date updatedAt;
}
