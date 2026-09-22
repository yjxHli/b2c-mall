package com.yjx.b2c.order.web.request;

import lombok.Data;

/**
 * @author yjx
 * @date 2026/9/17
 * @description 支付请求
 */
@Data
public class PayRequestVO {
    private Integer orderId;
    /** 支付方式：1-支付宝 2-微信 */
    private Integer payType;
}
