package com.yjx.b2c.employee.service.event.handler;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.employee.dao.mapper.LoginLogMapper;
import com.yjx.b2c.employee.dao.po.LoginLogPO;
import com.yjx.b2c.employee.service.event.EmployeeLoginEvent;
import com.yjx.b2c.employee.support.LoginFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 员工登录审计处理器（F4.1）。
 *
 * @author yjx
 * @date 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
class EmployeeAuditLogEventHandleTest {

    @Mock
    private EventBus eventBus;
    @Mock
    private LoginLogMapper loginLogMapper;
    @InjectMocks
    private EmployeeAuditLogEventHandle handler;

    @Test
    @DisplayName("4.1.1 处理器随容器注册到 EventBus")
    void registersWithEventBus() throws Exception {
        handler.afterPropertiesSet();

        verify(eventBus).register(handler);
    }

    @Test
    @DisplayName("4.1.2/4.1.3/4.1.4 事件映射为审计记录，userType=2")
    void mapsEventToLoginLog() {
        when(loginLogMapper.saveLoginLog(any(LoginLogPO.class))).thenReturn(1);

        handler.handler(LoginFixtures.loginEvent(33, 88, "admin"));

        ArgumentCaptor<LoginLogPO> captor = ArgumentCaptor.forClass(LoginLogPO.class);
        verify(loginLogMapper).saveLoginLog(captor.capture());
        LoginLogPO po = captor.getValue();
        assertThat(po.getUserId()).isEqualTo(33);
        assertThat(po.getUserType()).isEqualTo(2);
        assertThat(po.getLoginIp()).isEqualTo("1.1.1.1");
        assertThat(po.getLoginDevice()).isEqualTo("Windows 10/Chrome");
        assertThat(po.getUserAgent()).isEqualTo(LoginFixtures.USER_AGENT);
        assertThat(po.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("4.1.5 落库行数必须为 1")
    void rowCountMustBeOne() {
        when(loginLogMapper.saveLoginLog(any(LoginLogPO.class))).thenReturn(1);
        assertThatCode(() -> handler.handler(LoginFixtures.loginEvent(33, 88, "admin")))
                .doesNotThrowAnyException();

        when(loginLogMapper.saveLoginLog(any(LoginLogPO.class))).thenReturn(0);
        assertThatThrownBy(() -> handler.handler(LoginFixtures.loginEvent(33, 88, "admin")))
                .hasMessage("保存登录审计日志失败");
    }

    @Test
    @DisplayName("4.1.6 [红灯] clientInfo 为 null 时不得 NPE")
    void nullClientInfoDoesNotThrow() {
        EmployeeLoginEvent event = LoginFixtures.loginEvent(33, 88, "admin");
        event.setClientInfo(null);

        assertThatCode(() -> handler.handler(event)).doesNotThrowAnyException();
        verify(loginLogMapper, never()).saveLoginLog(any(LoginLogPO.class));
    }
}
