package com.yjx.b2c.product.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 商品状态
 */
@Getter
@AllArgsConstructor
public enum ProductStatusEnum {
    /** 草稿 */
    DRAFT(0, "草稿"),
    /** 已上架 */
    ON_SALE(1, "已上架"),
    /** 已下架 */
    OFF_SALE(2, "已下架");

    private final Integer code;
    private final String desc;
}
