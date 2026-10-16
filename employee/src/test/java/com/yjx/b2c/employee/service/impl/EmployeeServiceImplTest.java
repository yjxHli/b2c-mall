package com.yjx.b2c.employee.service.impl;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.employee.dao.mapper.EmployeeMapper;
import com.yjx.b2c.employee.dao.po.EmployeePO;
import com.yjx.b2c.common.contract.AddEmployeeRequestVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * employee 保存业务编排测试（任务项 4.4.2 - 4.4.5）。
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeMapper employeeMapper;
    @Mock
    private EventBus eventBus;
    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private static AddEmployeeRequestVO request(Integer shopId, String username, String password) {
        AddEmployeeRequestVO vo = new AddEmployeeRequestVO();
        vo.setShopId(shopId);
        vo.setUsername(username);
        vo.setPassword(password);
        return vo;
    }

    @Test
    @DisplayName("4.4.3/4.4.4 保存员工时写入默认状态与默认头像")
    void addEmployeeUsesDefaults() {
        when(employeeMapper.findByUsername("admin", 88)).thenReturn(null);
        when(employeeMapper.save(any(EmployeePO.class))).thenAnswer(invocation -> {
            EmployeePO po = invocation.getArgument(0);
            po.setId(7);
            return 1;
        });

        Integer id = employeeService.addEmployee(request(88, "admin", "123456"));

        assertThat(id).isEqualTo(7);
        ArgumentCaptor<EmployeePO> captor = ArgumentCaptor.forClass(EmployeePO.class);
        verify(employeeMapper).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getAvatarUrl()).isNotBlank();
    }

    @Test
    @DisplayName("4.4.2 缺少 shopId 时拒绝且不落库")
    void addEmployeeRejectsMissingShopId() {
        assertThatThrownBy(() -> employeeService.addEmployee(request(null, "admin", "123456")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(employeeMapper, never()).save(any(EmployeePO.class));
    }

    @Test
    @DisplayName("4.4.5 重复创建返回既有员工 ID，不新增记录")
    void addEmployeeIsIdempotent() {
        EmployeePO existing = new EmployeePO();
        existing.setId(7);
        when(employeeMapper.findByUsername("admin", 88)).thenReturn(existing);

        assertThat(employeeService.addEmployee(request(88, "admin", "123456"))).isEqualTo(7);
        verify(employeeMapper, never()).save(any(EmployeePO.class));
    }

    @Test
    @DisplayName("9.2 员工密码以 BCrypt 哈希存储")
    void addEmployeeHashesPassword() {
        when(employeeMapper.findByUsername("admin", 88)).thenReturn(null);
        when(employeeMapper.save(any(EmployeePO.class))).thenAnswer(invocation -> {
            EmployeePO po = invocation.getArgument(0);
            po.setId(7);
            return 1;
        });

        employeeService.addEmployee(request(88, "admin", "123456"));

        ArgumentCaptor<EmployeePO> captor = ArgumentCaptor.forClass(EmployeePO.class);
        verify(employeeMapper).save(captor.capture());
        String stored = captor.getValue().getPassword();
        assertThat(stored).isNotEqualTo("123456");
        assertThat(cn.hutool.crypto.digest.BCrypt.checkpw("123456", stored)).isTrue();
    }
}
