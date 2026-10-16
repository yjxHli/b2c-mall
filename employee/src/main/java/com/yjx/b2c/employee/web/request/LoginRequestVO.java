package com.yjx.b2c.employee.web.request;

import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author yjx
 * @date 2026/9/15
 * @description
 */
@Data
public class LoginRequestVO {
    @NotBlank(message = "账号不能为空")
    @Length(max = 50, message = "账号过长")
    private String username;
    @NotBlank(message = "账号密码不能为空")
    @ToString.Exclude
    private String password;
    /** 租户边界：缺省即拒绝，避免跨店铺检索同名账号 */
    @NotNull(message = "店铺不能为空")
    private Integer shopId;
}
