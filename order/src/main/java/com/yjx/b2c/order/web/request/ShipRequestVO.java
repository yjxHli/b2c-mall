package com.yjx.b2c.order.web.request;

import lombok.Data;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 发货请求
 */
@Data
public class ShipRequestVO {
    private Integer orderId;
    /** 物流单号 */
    private String logisticsNo;
    /** 操作人 */
    private Integer operatorId;
}
