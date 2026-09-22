package com.yjx.b2c.employee.service.event;

import com.yjx.b2c.common.bean.ClientInfo;
import lombok.Data;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 员工登录成功事件
 */
@Data
public class EmployeeLoginEvent {
    private Integer employeeId;
    private Integer shopId;
    private String username;
    private ClientInfo clientInfo;
}
