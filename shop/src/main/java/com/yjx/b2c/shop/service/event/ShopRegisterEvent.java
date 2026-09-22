package com.yjx.b2c.shop.service.event;

import lombok.Data;

/**
 * @author yjx
 * @date 2026/9/11
 * @description
 */
@Data
public class ShopRegisterEvent {
    private Integer shopId;
    private String shopName;
    private String adminAccount;
    private String adminPwd;
}
