package com.yjx.b2c.employee.web;

import com.yjx.b2c.common.bean.ClientInfo;
import com.yjx.b2c.common.response.BaseResponseVO;
import com.yjx.b2c.common.util.ClientInfoUtil;
import com.yjx.b2c.employee.service.EmployeeService;
import com.yjx.b2c.employee.web.request.AddEmployeeRequestVO;
import com.yjx.b2c.employee.web.request.LoginRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    @PostMapping("/save")
    public BaseResponseVO<Integer> save(@RequestBody @Validated AddEmployeeRequestVO req) {
        Integer id = employeeService.addEmployee(req);
        return new BaseResponseVO<>(id);
    }

    @PostMapping("/login")
    public BaseResponseVO<Integer> login(@RequestBody @Validated LoginRequestVO req, HttpServletRequest request) {
        ClientInfo clientInfo = ClientInfoUtil.of(request);
        Integer id = employeeService.login(req, clientInfo);
        return new BaseResponseVO<>(id);
    }

}