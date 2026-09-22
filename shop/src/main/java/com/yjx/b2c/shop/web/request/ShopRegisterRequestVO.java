package com.yjx.b2c.shop.web.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * @author yjx
 * @date 2026/9/10
 * @description
 */
@Data
public class ShopRegisterRequestVO {
    @NotBlank(message = "店铺名不能为空")
    @Length(max = 6, message = "店铺名过长")
    private String shopName;
    @NotBlank(message = "账户不能为空")
    private String adminAccount;
    @NotBlank(message = "账户密码")
    private String adminPassword;
}