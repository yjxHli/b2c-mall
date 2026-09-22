package com.yjx.b2c.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 订单状态（流转规则由状态模式的各个状态类负责）
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {
    /** 待支付 */
    PENDING_PAY(0, "待支付"),
    /** 已支付 */
    PAID(1, "已支付"),
    /** 已发货 */
    SHIPPED(2, "已发货"),
    /** 已完成 */
    COMPLETED(3, "已完成"),
    /** 已取消 */
    CANCELLED(4, "已取消");

    private final Integer code;
    private final String desc;

    public static OrderStatus of(Integer code) {
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
