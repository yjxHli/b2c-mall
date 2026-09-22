package com.yjx.b2c.order.bean;

import lombok.Data;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 支付结果
 */
@Data
public class PayResult {
    private boolean success;
    private String tradeNo;
    private String message;

    public static PayResult success(String tradeNo) {
        PayResult result = new PayResult();
        result.setSuccess(true);
        result.setTradeNo(tradeNo);
        result.setMessage("支付成功");
        return result;
    }

    public static PayResult fail(String message) {
        PayResult result = new PayResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}
