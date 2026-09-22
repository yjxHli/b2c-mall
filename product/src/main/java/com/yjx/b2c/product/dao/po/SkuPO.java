package com.yjx.b2c.product.dao.po;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author yjx
 * @date 2026/9/15
 * @description 商品（SKU）
 */
@Data
public class SkuPO {
    private Integer id;
    private Integer shopId;
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
    /** 0-草稿 1-上架 2-下架 */
    private Integer status;
    private Integer operatorId;
    private Date createdAt;
    private Date updatedAt;
}
