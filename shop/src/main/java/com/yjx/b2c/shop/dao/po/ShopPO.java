package com.yjx.b2c.shop.dao.po;

import lombok.Data;

import java.util.Date;

/**
 * @author yjx
 * @date 2026/9/10
 * @description
 */
@Data
public class ShopPO {
    private Integer id;
    private String shopName;
    private String adminAccount;
    private String adminPassword;
    private String logoUrl;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
}