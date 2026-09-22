package com.yjx.b2c.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yjx
 * @date 2026/9/17
 * @description 支付方式
 */
@Getter
@AllArgsConstructor
public enum PayTypeEnum {
    /** 支付宝 */
    ALIPAY(1, "支付宝"),
    /** 微信支付 */
    WECHAT(2, "微信支付");

    private final Integer code;
    private final String desc;

    public static PayTypeEnum of(Integer code) {
        for (PayTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
