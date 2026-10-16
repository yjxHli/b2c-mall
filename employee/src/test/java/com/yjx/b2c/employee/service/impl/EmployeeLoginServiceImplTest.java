package com.yjx.b2c.employee.service.impl;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.employee.dao.mapper.EmployeeMapper;
import com.yjx.b2c.employee.dao.po.EmployeePO;
import com.yjx.b2c.employee.service.event.EmployeeLoginEvent;
import com.yjx.b2c.employee.support.LoginFixtures;
import com.yjx.b2c.employee.web.request.LoginRequestVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.yjx.b2c.employee.support.LoginFixtures.PASSWORD;
import static com.yjx.b2c.employee.support.LoginFixtures.SHOP_ID;
import static com.yjx.b2c.employee.support.LoginFixtures.USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 员工登录的 Service 层行为（F1.3 / F2.2 / F3.1 / F6.2）。
 *
 * @author yjx
 * @date 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
class EmployeeLoginServiceImplTest {

    @Mock
    private EmployeeMapper employeeMapper;
    @Mock
    private EventBus eventBus;
    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private LoginRequestVO request(String username, String password) {
        return LoginFixtures.loginRequest(username, password, SHOP_ID);
    }

    @Test
    @DisplayName("1.3.1 空白账号被拒绝且不查询数据库")
    void blankUsernameRejected() {
        for (String blank : new String[]{null, "", "   "}) {
            assertThatThrownBy(() -> employeeService.login(
                    LoginFixtures.loginRequest(blank, PASSWORD, SHOP_ID), LoginFixtures.clientInfo()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verify(employeeMapper, never()).findByUsername(any(), anyInt());
    }

    @Test
    @DisplayName("1.3.2 空白密码被拒绝且不查询数据库")
    void blankPasswordRejected() {
        for (String blank : new String[]{null, "", "   "}) {
            assertThatThrownBy(() -> employeeService.login(
                    LoginFixtures.loginRequest(USERNAME, blank, SHOP_ID), LoginFixtures.clientInfo()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verify(employeeMapper, never()).findByUsername(any(), anyInt());
    }

    @Test
    @DisplayName("1.3.3 用账号 + 店铺查询")
    void queriesByUsernameAndShop() {
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID))
                .thenReturn(LoginFixtures.employeePO(1, SHOP_ID, USERNAME, PASSWORD, 1));

        employeeService.login(request(USERNAME, PASSWORD), LoginFixtures.clientInfo());

        verify(employeeMapper).findByUsername(USERNAME, SHOP_ID);
    }

    @Test
    @DisplayName("1.3.4 用 BCrypt 比对明文密码")
    void comparesBcryptHash() {
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID))
                .thenReturn(LoginFixtures.employeePO(11, SHOP_ID, USERNAME, PASSWORD, 1));

        assertThat(employeeService.login(request(USERNAME, PASSWORD), LoginFixtures.clientInfo())).isEqualTo(11);
    }

    @Test
    @DisplayName("1.3.5 密码比对大小写敏感")
    void passwordCaseSensitive() {
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID))
                .thenReturn(LoginFixtures.employeePO(1, SHOP_ID, USERNAME, "Abc123", 1));

        assertThatThrownBy(() -> employeeService.login(request(USERNAME, "abc123"), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("账号或密码错误");
    }

    @Test
    @DisplayName("1.3.6 [红灯] 库中非 BCrypt 口令不泄露加密内部错误")
    void nonBcryptPasswordFailsAsCredentialError() {
        EmployeePO legacy = new EmployeePO();
        legacy.setId(1);
        legacy.setShopId(SHOP_ID);
        legacy.setUsername(USERNAME);
        legacy.setPassword("123456");
        legacy.setStatus(1);
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID)).thenReturn(legacy);

        assertThatThrownBy(() -> employeeService.login(request(USERNAME, PASSWORD), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("账号或密码错误");
    }

    @Test
    @DisplayName("1.3.7/1.3.8 校验通过后发布字段完整的登录事件")
    void publishesCompleteEvent() {
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID))
                .thenReturn(LoginFixtures.employeePO(11, SHOP_ID, USERNAME, PASSWORD, 1));

        employeeService.login(request(USERNAME, PASSWORD), LoginFixtures.clientInfo());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventBus).post(captor.capture());
        EmployeeLoginEvent event = (EmployeeLoginEvent) captor.getValue();
        assertThat(event.getEmployeeId()).isEqualTo(11);
        assertThat(event.getShopId()).isEqualTo(SHOP_ID);
        assertThat(event.getUsername()).isEqualTo(USERNAME);
        assertThat(event.getClientInfo()).isEqualTo(LoginFixtures.clientInfo());
    }

    @Test
    @DisplayName("1.3.9 clientInfo 为 null 时 Service 仍返回成功")
    void nullClientInfoDoesNotBreakService() {
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID))
                .thenReturn(LoginFixtures.employeePO(11, SHOP_ID, USERNAME, PASSWORD, 1));

        assertThat(employeeService.login(request(USERNAME, PASSWORD), null)).isEqualTo(11);
        verify(eventBus).post(any());
    }

    @Test
    @DisplayName("1.3.10 查询异常向上传播")
    void mapperExceptionPropagates() {
        RuntimeException boom = new RuntimeException("db down");
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID)).thenThrow(boom);

        assertThatThrownBy(() -> employeeService.login(request(USERNAME, PASSWORD), LoginFixtures.clientInfo()))
                .isSameAs(boom);
    }

    @Test
    @DisplayName("1.3.11 返回库中主键")
    void returnsPersistedId() {
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID))
                .thenReturn(LoginFixtures.employeePO(77, SHOP_ID, USERNAME, PASSWORD, 1));

        assertThat(employeeService.login(request(USERNAME, PASSWORD), LoginFixtures.clientInfo())).isEqualTo(77);
    }

    @Test
    @DisplayName("2.2.1/2.2.2/2.2.3 账号不存在与密码错误不可区分且不发事件")
    void failuresAreIndistinguishable() {
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID)).thenReturn(null);
        String unknown = failureMessage(request(USERNAME, PASSWORD));

        org.mockito.Mockito.reset(employeeMapper);
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID))
                .thenReturn(LoginFixtures.employeePO(1, SHOP_ID, USERNAME, PASSWORD, 1));
        String wrong = failureMessage(request(USERNAME, "wrong-pass"));

        assertThat(unknown).isEqualTo(wrong).isEqualTo("账号或密码错误");
        verify(eventBus, never()).post(any());
    }

    private String failureMessage(LoginRequestVO vo) {
        try {
            employeeService.login(vo, LoginFixtures.clientInfo());
            return "<no-exception>";
        } catch (RuntimeException ex) {
            return ex.getMessage();
        }
    }

    @Test
    @DisplayName("2.2.4 失败路径无写副作用")
    void failureHasNoSideEffects() {
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID))
                .thenReturn(LoginFixtures.employeePO(1, SHOP_ID, USERNAME, PASSWORD, 1));

        assertThatThrownBy(() -> employeeService.login(request(USERNAME, "wrong-pass"), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(employeeMapper, never()).updateLoginStatus(anyInt());
        verify(eventBus, never()).post(any());
    }

    @Test
    @DisplayName("2.2.6 [红灯] 明文口令不出现在 VO 的 toString 中")
    void toStringDoesNotExposePassword() {
        assertThat(request(USERNAME, PASSWORD).toString()).doesNotContain(PASSWORD);
    }

    @Test
    @DisplayName("3.1.1/3.1.6 status=0 时拒绝并给出员工专属文案")
    void disabledEmployeeRejected() {
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID))
                .thenReturn(LoginFixtures.employeePO(1, SHOP_ID, USERNAME, PASSWORD, 0));

        assertThatThrownBy(() -> employeeService.login(request(USERNAME, PASSWORD), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("账号已被禁用");
    }

    @Test
    @DisplayName("3.1.2 status=1 允许登录")
    void enabledEmployeeAllowed() {
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID))
                .thenReturn(LoginFixtures.employeePO(5, SHOP_ID, USERNAME, PASSWORD, 1));

        assertThat(employeeService.login(request(USERNAME, PASSWORD), LoginFixtures.clientInfo())).isEqualTo(5);
    }

    @Test
    @DisplayName("3.1.3 禁用账号不发布事件")
    void disabledPublishesNoEvent() {
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID))
                .thenReturn(LoginFixtures.employeePO(1, SHOP_ID, USERNAME, PASSWORD, 0));

        assertThatThrownBy(() -> employeeService.login(request(USERNAME, PASSWORD), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(eventBus, never()).post(any());
    }

    @Test
    @DisplayName("3.1.4 状态判定在密码校验之后")
    void disabledCheckRunsAfterPasswordCheck() {
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID))
                .thenReturn(LoginFixtures.employeePO(1, SHOP_ID, USERNAME, PASSWORD, 0));

        assertThatThrownBy(() -> employeeService.login(request(USERNAME, "wrong-pass"), LoginFixtures.clientInfo()))
                .hasMessage("账号或密码错误");
    }

    @Test
    @DisplayName("3.1.5 status 为 null 时按拒绝处理")
    void nullStatusRejected() {
        when(employeeMapper.findByUsername(USERNAME, SHOP_ID))
                .thenReturn(LoginFixtures.employeePO(1, SHOP_ID, USERNAME, PASSWORD, null));

        assertThatThrownBy(() -> employeeService.login(request(USERNAME, PASSWORD), LoginFixtures.clientInfo()))
                .hasMessage("账号已被禁用");
    }

    @Test
    @DisplayName("6.2.1/6.2.2 [红灯] 缺少 shopId 时拒绝且不跨店检索")
    void missingShopIdRejected() {
        assertThatThrownBy(() -> employeeService.login(
                LoginFixtures.loginRequest(USERNAME, PASSWORD, null), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("店铺不能为空");
        verify(employeeMapper, never()).findByUsername(any(), any());
    }

    @Test
    @DisplayName("6.2.3 shopId 与账号所属店铺不匹配时统一失败")
    void shopMismatchFailsGenerically() {
        when(employeeMapper.findByUsername(USERNAME, 999)).thenReturn(null);

        assertThatThrownBy(() -> employeeService.login(
                LoginFixtures.loginRequest(USERNAME, PASSWORD, 999), LoginFixtures.clientInfo()))
                .hasMessage("账号或密码错误");
    }

    @Test
    @DisplayName("6.2.4 同一 username 在不同店铺各自登录互不影响")
    void sameUsernameIsolatedPerShop() {
        when(employeeMapper.findByUsername(USERNAME, 1)).thenReturn(LoginFixtures.employeePO(11, 1, USERNAME, PASSWORD, 1));
        when(employeeMapper.findByUsername(USERNAME, 2)).thenReturn(LoginFixtures.employeePO(22, 2, USERNAME, PASSWORD, 1));

        assertThat(employeeService.login(LoginFixtures.loginRequest(USERNAME, PASSWORD, 1),
                LoginFixtures.clientInfo())).isEqualTo(11);
        assertThat(employeeService.login(LoginFixtures.loginRequest(USERNAME, PASSWORD, 2),
                LoginFixtures.clientInfo())).isEqualTo(22);

        verify(employeeMapper).findByUsername(eq(USERNAME), eq(1));
        verify(employeeMapper).findByUsername(eq(USERNAME), eq(2));
    }
}
