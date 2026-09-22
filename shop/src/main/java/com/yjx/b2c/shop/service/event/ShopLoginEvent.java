package com.yjx.b2c.shop.service.event;

import com.yjx.b2c.common.bean.ClientInfo;
import lombok.Data;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 店铺登录成功事件
 */
@Data
public class ShopLoginEvent {
    private Integer shopId;
    private String shopName;
    private String adminAccount;
    private ClientInfo clientInfo;
}
