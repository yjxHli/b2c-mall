package com.yjx.b2c.employee.dao.po;

import lombok.Data;

import java.util.Date;

/**
 * @author yjx
 * @date 2026/9/15
 * @description
 */
@Data
public class EmployeePO {
    private Integer id;
    private Integer shopId;
    private String username;
    private String password;
    private String avatarUrl;
    private Date lastLoginTime;
    private Integer loginCount;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
}
