package com.yjx.b2c.employee.service.impl;


import cn.hutool.core.lang.Assert;
import com.google.common.eventbus.EventBus;
import com.yjx.b2c.common.bean.ClientInfo;
import com.yjx.b2c.employee.dao.mapper.EmployeeMapper;
import com.yjx.b2c.employee.dao.po.EmployeePO;
import com.yjx.b2c.employee.service.EmployeeService;
import com.yjx.b2c.employee.service.event.EmployeeLoginEvent;
import com.yjx.b2c.employee.web.request.AddEmployeeRequestVO;
import com.yjx.b2c.employee.web.request.LoginRequestVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {
    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private EventBus eventBus;

    @Override
    public Integer addEmployee(AddEmployeeRequestVO vo) {
        Assert.notBlank(vo.getUsername());
        Assert.notNull(vo.getShopId());
        Assert.notBlank(vo.getPassword());

        EmployeePO po = new EmployeePO();
        BeanUtils.copyProperties(vo, po);
        po.setAvatarUrl("http://47.100.22.158:8848/nacos/img/logo-2000-390.svg");
        po.setStatus(1);

        Integer row = employeeMapper.save(po);
        Assert.equals(row, 1, "保存员工信息失败");
        return po.getId();
    }

    @Override
    public Integer login(LoginRequestVO vo, ClientInfo clientInfo) {
        Assert.notBlank(vo.getUsername(), "账号不能为空");
        Assert.notBlank(vo.getPassword(), "账号密码不能为空");

        EmployeePO employee = employeeMapper.findByUsername(vo.getUsername(), vo.getShopId());
        Assert.notNull(employee, "账号或密码错误");
        Assert.equals(employee.getPassword(), vo.getPassword(), "账号或密码错误");
        Assert.equals(employee.getStatus(), 1, "账号已被禁用");

        EmployeeLoginEvent event = new EmployeeLoginEvent();
        event.setEmployeeId(employee.getId());
        event.setShopId(employee.getShopId());
        event.setUsername(employee.getUsername());
        event.setClientInfo(clientInfo);
        eventBus.post(event);
        return employee.getId();
    }
}