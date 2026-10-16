package com.yjx.b2c.employee.service.event.handler;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.employee.dao.mapper.EmployeeMapper;
import com.yjx.b2c.employee.support.LoginFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 员工登录状态处理器（F5.1）。
 *
 * @author yjx
 * @date 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
class EmployeeUserStatusEventHandleTest {

    @Mock
    private EventBus eventBus;
    @Mock
    private EmployeeMapper employeeMapper;
    @InjectMocks
    private EmployeeUserStatusEventHandle handler;

    @Test
    @DisplayName("5.1.1 处理器随容器注册到 EventBus")
    void registersWithEventBus() throws Exception {
        handler.afterPropertiesSet();

        verify(eventBus).register(handler);
    }

    @Test
    @DisplayName("5.1.2 用事件中的 employeeId 更新（不是 shopId）")
    void updatesUsingEmployeeId() {
        when(employeeMapper.updateLoginStatus(33)).thenReturn(1);

        handler.handler(LoginFixtures.loginEvent(33, 88, "admin"));

        verify(employeeMapper).updateLoginStatus(33);
    }

    @Test
    @DisplayName("5.1.3 更新行数必须为 1")
    void rowCountMustBeOne() {
        when(employeeMapper.updateLoginStatus(33)).thenReturn(1);
        assertThatCode(() -> handler.handler(LoginFixtures.loginEvent(33, 88, "admin")))
                .doesNotThrowAnyException();

        when(employeeMapper.updateLoginStatus(33)).thenReturn(0);
        assertThatThrownBy(() -> handler.handler(LoginFixtures.loginEvent(33, 88, "admin")))
                .hasMessage("更新员工登录状态失败");
    }

    @Test
    @DisplayName("5.1.5 状态处理器不依赖 clientInfo")
    void doesNotDependOnClientInfo() {
        when(employeeMapper.updateLoginStatus(33)).thenReturn(1);
        com.yjx.b2c.employee.service.event.EmployeeLoginEvent event =
                LoginFixtures.loginEvent(33, 88, "admin");
        event.setClientInfo(null);

        assertThatCode(() -> handler.handler(event)).doesNotThrowAnyException();
        verify(employeeMapper).updateLoginStatus(33);
    }
}
