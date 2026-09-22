package com.yjx.b2c.product.dao.po;

import lombok.Data;

import java.util.Date;

/**
 * @author yjx
 * @date 2026/9/15
 * @description 商品操作日志
 */
@Data
public class SkuLogPO {
    private Integer id;
    private Integer skuId;
    private Integer shopId;
    private Integer operatorId;
    private String action;
    private String detail;
    private Date createdAt;
}
