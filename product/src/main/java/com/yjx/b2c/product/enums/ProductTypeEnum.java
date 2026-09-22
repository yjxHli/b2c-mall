package com.yjx.b2c.product.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 商品类型
 */
@Getter
@AllArgsConstructor
public enum ProductTypeEnum {
    /** 实物商品：校验库存 */
    PHYSICAL(1, "实物商品"),
    /** 虚拟商品：校验合规 */
    VIRTUAL(2, "虚拟商品");

    private final Integer code;
    private final String desc;

    public static ProductTypeEnum of(Integer code) {
        for (ProductTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
