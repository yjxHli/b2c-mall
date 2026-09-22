package com.yjx.b2c.order.bean;

import com.yjx.b2c.order.dao.po.OrderPO;
import com.yjx.b2c.order.enums.PayTypeEnum;
import lombok.Data;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 订单操作上下文：状态模式在流转时携带的数据
 */
@Data
public class OrderContext {
    /** 订单id */
    private Integer orderId;
    /** 订单（由状态机加载后放入） */
    private OrderPO order;
    /** 支付方式 */
    private PayTypeEnum payType;
    /** 支付流水号（支付策略返回） */
    private String tradeNo;
    /** 物流单号（发货用） */
    private String logisticsNo;
    /** 操作人 */
    private Integer operatorId;
}
