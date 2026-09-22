package com.yjx.b2c.product.web.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author yjx
 * @date 2026/9/15
 * @description 新建商品请求
 */
@Data
public class CreateProductRequest {
    private Integer shopId;
    private Integer operatorId;
    private Integer categoryId;
    private String productName;
    private String productDesc;
    /** 1-实物 2-虚拟 */
    private Integer productType;
    private BigDecimal price;
    private Integer stock;
    /** 虚拟商品交付内容/链接 */
    private String deliveryContent;
    private String coverUrl;
}
