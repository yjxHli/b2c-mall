package com.yjx.b2c.employee.web.request;

import lombok.Data;

/**
 * @author yjx
 * @date 2026/9/15
 * @description
 */
@Data
public class LoginRequestVO {
    private String username;
    private String password;
    private Integer shopId;
}

