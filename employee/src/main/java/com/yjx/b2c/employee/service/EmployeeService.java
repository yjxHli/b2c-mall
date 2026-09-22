package com.yjx.b2c.employee.service;

import com.yjx.b2c.common.bean.ClientInfo;
import com.yjx.b2c.employee.web.request.AddEmployeeRequestVO;
import com.yjx.b2c.employee.web.request.LoginRequestVO;

/**
 * @author yjx
 * @date 2026/9/15
 * @description
 */

public interface EmployeeService {
    Integer addEmployee(AddEmployeeRequestVO vo);

    Integer login(LoginRequestVO vo, ClientInfo clientInfo);
}

