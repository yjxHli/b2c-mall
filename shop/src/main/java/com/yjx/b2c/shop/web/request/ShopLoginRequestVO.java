package com.yjx.b2c.shop.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 店铺登录请求
 */
@Data
public class ShopLoginRequestVO {
    @NotBlank(message = "账户不能为空")
    private String adminAccount;
    @NotBlank(message = "账户密码不能为空")
    private String adminPassword;
}
