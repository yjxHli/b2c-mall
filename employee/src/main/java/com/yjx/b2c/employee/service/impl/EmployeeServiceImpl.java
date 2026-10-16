package com.yjx.b2c.employee.service.impl;


import cn.hutool.core.lang.Assert;
import cn.hutool.crypto.digest.BCrypt;
import com.google.common.eventbus.EventBus;
import com.yjx.b2c.common.bean.ClientInfo;
import com.yjx.b2c.common.exception.BizException;
import com.yjx.b2c.employee.dao.mapper.EmployeeMapper;
import com.yjx.b2c.employee.dao.po.EmployeePO;
import com.yjx.b2c.employee.service.EmployeeService;
import com.yjx.b2c.employee.service.event.EmployeeLoginEvent;
import com.yjx.b2c.common.contract.AddEmployeeRequestVO;
import com.yjx.b2c.employee.web.request.LoginRequestVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        // 幂等：事件重放或补偿重试时返回既有账号，避免重复创建
        EmployeePO existing = employeeMapper.findByUsername(vo.getUsername(), vo.getShopId());
        if (existing != null) {
            return existing.getId();
        }

        EmployeePO po = new EmployeePO();
        BeanUtils.copyProperties(vo, po);
        po.setPassword(BCrypt.hashpw(vo.getPassword(), BCrypt.gensalt()));
        po.setAvatarUrl("http://47.100.22.158:8848/nacos/img/logo-2000-390.svg");
        po.setStatus(1);

        Integer row = employeeMapper.save(po);
        if (row == null || row != 1) {
            throw BizException.serverError("保存员工信息失败");
        }
        return po.getId();
    }

    @Override
    public Integer login(LoginRequestVO vo, ClientInfo clientInfo) {
        // 租户边界优先校验：shopId 为空时按账号检索会跨店铺命中
        Assert.notNull(vo.getShopId(), "店铺不能为空");
        Assert.notBlank(vo.getUsername(), "账号不能为空");
        Assert.notBlank(vo.getPassword(), "账号密码不能为空");

        EmployeePO employee = employeeMapper.findByUsername(vo.getUsername(), vo.getShopId());
        Assert.notNull(employee, "账号或密码错误");
        Assert.isTrue(BCrypt.checkpw(vo.getPassword(), employee.getPassword()), "账号或密码错误");
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
