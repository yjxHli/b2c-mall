package com.yjx.b2c.employee.service.event.handler;

import cn.hutool.core.lang.Assert;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.yjx.b2c.employee.dao.mapper.EmployeeMapper;
import com.yjx.b2c.employee.service.event.EmployeeLoginEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 用户状态观察者：更新员工最后登录时间与累计登录次数
 */
@Component
@Slf4j
public class EmployeeUserStatusEventHandle implements InitializingBean {
    @Autowired
    private EventBus eventBus;
    @Autowired
    private EmployeeMapper employeeMapper;

    @Subscribe
    public void handler(EmployeeLoginEvent event) {
        int row = employeeMapper.updateLoginStatus(event.getEmployeeId());
        Assert.equals(row, 1, "更新员工登录状态失败");
        log.info("员工登录状态已更新, employeeId={}", event.getEmployeeId());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        eventBus.register(this);
    }
}
