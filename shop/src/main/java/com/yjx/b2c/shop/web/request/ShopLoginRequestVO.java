package com.yjx.b2c.shop.web.request;

import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 店铺登录请求
 */
@Data
public class ShopLoginRequestVO {
    @NotBlank(message = "账户不能为空")
    @Length(max = 100, message = "账户过长")
    private String adminAccount;
    @NotBlank(message = "账户密码不能为空")
    @Length(max = 100, message = "密码过长")
    @ToString.Exclude
    private String adminPassword;
}
