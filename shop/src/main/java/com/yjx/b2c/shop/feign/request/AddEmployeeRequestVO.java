package com.yjx.b2c.shop.feign.request;

import lombok.Data;

/**
 * @author yjx
 * @date 2026/9/11
 * @description
 */
@Data
public class AddEmployeeRequestVO {

    private Integer shopId;
    private String username;
    private String password;
}
