package com.yjx.b2c.employee.dao.mapper;

import com.yjx.b2c.employee.dao.po.LoginLogPO;
import com.yjx.b2c.employee.support.EmployeeDaoTestSupport;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 员工登录审计日志持久化（F4.2）。
 *
 * @author yjx
 * @date 2026/10/16
 */
class EmployeeLoginLogMapperTest extends EmployeeDaoTestSupport {

    private Object scalar(String sql) throws Exception {
        try (Statement statement = session.getConnection().createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getObject(1) : null;
        }
    }

    private static LoginLogPO loginLog(Integer userId) {
        LoginLogPO po = new LoginLogPO();
        po.setUserId(userId);
        po.setUserType(2);
        po.setLoginIp("1.1.1.1");
        po.setLoginDevice("Windows 10/Chrome");
        po.setLoginLocation("未知");
        po.setUserAgent("UA/1.0");
        po.setStatus(1);
        return po;
    }

    @Test
    @DisplayName("4.2.1 写入审计日志的八个业务列（userType=2）")
    void insertCoversAllColumns() throws Exception {
        LoginLogPO po = loginLog(33);

        assertThat(loginLogMapper.saveLoginLog(po)).isEqualTo(1);

        try (Statement statement = session.getConnection().createStatement();
             ResultSet rs = statement.executeQuery("SELECT user_id, user_type, login_ip, login_device, "
                     + "login_location, login_time, user_agent, status FROM tb_login_logs WHERE id = " + po.getId())) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("user_id")).isEqualTo(33);
            assertThat(rs.getInt("user_type")).isEqualTo(2);
            assertThat(rs.getString("login_ip")).isEqualTo("1.1.1.1");
            assertThat(rs.getString("login_time")).isNotNull();
            assertThat(rs.getString("user_agent")).isEqualTo("UA/1.0");
            assertThat(rs.getInt("status")).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("4.2.2 主键回填到 LoginLogPO.id")
    void backfillsGeneratedId() throws Exception {
        LoginLogPO po = loginLog(34);

        loginLogMapper.saveLoginLog(po);

        assertThat(po.getId()).isNotNull();
        assertThat(po.getId()).isEqualTo(((Number) scalar("SELECT last_insert_rowid()")).intValue());
    }

    @Test
    @DisplayName("4.2.3 user_id 的 NOT NULL 约束生效")
    void userIdIsNotNull() {
        assertThatThrownBy(() -> loginLogMapper.saveLoginLog(loginLog(null)))
                .isInstanceOf(PersistenceException.class)
                .hasMessageContaining("NOT NULL");
    }
}
