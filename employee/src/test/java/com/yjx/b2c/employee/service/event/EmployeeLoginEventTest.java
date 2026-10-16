package com.yjx.b2c.employee.service.event;

import com.yjx.b2c.employee.support.LoginFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 员工登录成功事件契约（F1.5）。
 *
 * @author yjx
 * @date 2026/10/16
 */
class EmployeeLoginEventTest {

    @Test
    @DisplayName("1.5.2 EmployeeLoginEvent 携带员工、店铺与客户端信息")
    void carriesEmployeeIdentity() {
        EmployeeLoginEvent event = new EmployeeLoginEvent();
        event.setEmployeeId(33);
        event.setShopId(88);
        event.setUsername("admin");
        event.setClientInfo(LoginFixtures.clientInfo());

        assertThat(event.getEmployeeId()).isEqualTo(33);
        assertThat(event.getShopId()).isEqualTo(88);
        assertThat(event.getUsername()).isEqualTo("admin");
        assertThat(event.getClientInfo()).isEqualTo(LoginFixtures.clientInfo());
    }
}
