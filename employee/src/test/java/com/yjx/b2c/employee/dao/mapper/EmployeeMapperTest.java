package com.yjx.b2c.employee.dao.mapper;

import com.yjx.b2c.employee.dao.po.EmployeePO;
import com.yjx.b2c.employee.support.EmployeeDaoTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * employee 持久化测试（任务项 4.5.x）。
 */
class EmployeeMapperTest extends EmployeeDaoTestSupport {

    private EmployeePO employee(Integer shopId, String username) {
        EmployeePO po = new EmployeePO();
        po.setShopId(shopId);
        po.setUsername(username);
        po.setPassword("123456");
        po.setAvatarUrl("/avatars/default.png");
        po.setStatus(1);
        return po;
    }

    @Test
    @DisplayName("4.5.1/4.5.2 保存员工并回填自增主键")
    void saveAndBackfillId() {
        EmployeePO po = employee(88, "admin");

        assertThat(employeeMapper.save(po)).isEqualTo(1);
        assertThat(po.getId()).isNotNull().isPositive();

        EmployeePO saved = employeeMapper.findByUsername("admin", 88);
        assertThat(saved.getShopId()).isEqualTo(88);
        assertThat(saved.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("4.5.3 findByUsername 在 shopId 为 null 时忽略该条件")
    void findByUsernameRespectsShopId() {
        employeeMapper.save(employee(88, "admin"));
        employeeMapper.save(employee(99, "admin"));

        assertThat(employeeMapper.findByUsername("admin", 99).getShopId()).isEqualTo(99);
        assertThat(employeeMapper.findByUsername("admin", 88).getShopId()).isEqualTo(88);
        assertThat(employeeMapper.findByUsername("admin", null)).isNotNull();
    }

    @Test
    @DisplayName("4.4.5/4.5.4 [红灯] 同一店铺同一账号只能存在一条")
    void duplicateEmployeeRejected() {
        employeeMapper.save(employee(88, "admin"));

        assertThatThrownBy(() -> employeeMapper.save(employee(88, "admin")))
                .hasMessageContaining("UNIQUE constraint failed");
    }
}
