package com.yjx.b2c.employee.dao.mapper;

import com.yjx.b2c.employee.dao.po.EmployeePO;
import com.yjx.b2c.employee.support.EmployeeDaoTestSupport;
import com.yjx.b2c.employee.support.LoginFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;

import static com.yjx.b2c.employee.support.LoginFixtures.PASSWORD;
import static com.yjx.b2c.employee.support.LoginFixtures.USERNAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 员工账号查询与登录状态更新的 SQL 语义（F1.4 / F3.2 / F5.2 / F6.2）。
 *
 * @author yjx
 * @date 2026/10/16
 */
class EmployeeLoginMapperTest extends EmployeeDaoTestSupport {

    private EmployeePO insert(String username, int shopId, String rawPassword) {
        EmployeePO po = LoginFixtures.employeePO(null, shopId, username, rawPassword, 1);
        employeeMapper.save(po);
        return po;
    }

    private Object scalar(String sql) throws Exception {
        try (Statement statement = session.getConnection().createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getObject(1) : null;
        }
    }

    private void execute(String sql) throws Exception {
        try (Statement statement = session.getConnection().createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Test
    @DisplayName("1.4.2 findByUsername 按账号等值查询")
    void findByUsernameMatches() {
        insert("admin", 1, PASSWORD);

        EmployeePO found = employeeMapper.findByUsername("admin", 1);
        assertThat(found).isNotNull();
        assertThat(found.getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("1.4.3 查询结果字段完整")
    void mapsAllColumns() {
        EmployeePO inserted = insert("admin", 7, PASSWORD);

        EmployeePO found = employeeMapper.findByUsername("admin", 7);
        assertThat(found.getId()).isEqualTo(inserted.getId());
        assertThat(found.getShopId()).isEqualTo(7);
        assertThat(found.getUsername()).isEqualTo("admin");
        assertThat(found.getPassword()).startsWith("$2");
        assertThat(found.getAvatarUrl()).isNotBlank();
        assertThat(found.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("1.4.4 查询不到的账号返回 null")
    void missingReturnsNull() {
        assertThat(employeeMapper.findByUsername("nobody", 1)).isNull();
    }

    @Test
    @DisplayName("1.4.5 账号比较大小写敏感")
    void caseSensitive() {
        insert("Admin", 1, PASSWORD);

        assertThat(employeeMapper.findByUsername("admin", 1)).isNull();
        assertThat(employeeMapper.findByUsername("Admin", 1)).isNotNull();
    }

    @Test
    @DisplayName("6.2.3 shopId 参与过滤：跨店同名账号互不可见")
    void shopIdScopesLookup() {
        insert("admin", 1, PASSWORD);
        insert("admin", 2, PASSWORD);

        assertThat(employeeMapper.findByUsername("admin", 1).getShopId()).isEqualTo(1);
        assertThat(employeeMapper.findByUsername("admin", 2).getShopId()).isEqualTo(2);
        assertThat(employeeMapper.findByUsername("admin", 999)).isNull();
    }

    @Test
    @DisplayName("6.3.2 (shop_id, username) 唯一索引存在")
    void uniqueIndexExists() throws Exception {
        assertThat(scalar("SELECT count(1) FROM sqlite_master WHERE type = 'index' "
                + "AND name = 'uk_employee_shop_username'")).isEqualTo(1);
    }

    @Test
    @DisplayName("3.2.1 查询返回 status 列")
    void returnsStatus() throws Exception {
        insert("admin", 1, PASSWORD);
        execute("UPDATE tb_employee SET status = 0 WHERE username = 'admin'");

        assertThat(employeeMapper.findByUsername("admin", 1).getStatus()).isZero();
    }

    @Test
    @DisplayName("3.2.2 建表默认 status=1")
    void defaultStatusIsOne() throws Exception {
        execute("INSERT INTO tb_employee (shop_id, username, password) VALUES (1, 'default-status', 'x')");

        assertThat(scalar("SELECT status FROM tb_employee WHERE username = 'default-status'")).isEqualTo(1);
    }

    @Test
    @DisplayName("5.2.1/5.2.2 登录状态更新：计数自增、最后登录时间刷新")
    void updateLoginStatusIncrements() throws Exception {
        EmployeePO po = insert(USERNAME, 1, PASSWORD);

        assertThat(employeeMapper.updateLoginStatus(po.getId())).isEqualTo(1);
        assertThat(scalar("SELECT login_count FROM tb_employee WHERE id = " + po.getId())).isEqualTo(1);
        assertThat(scalar("SELECT last_login_time FROM tb_employee WHERE id = " + po.getId())).isNotNull();

        assertThat(employeeMapper.updateLoginStatus(po.getId())).isEqualTo(1);
        assertThat(scalar("SELECT login_count FROM tb_employee WHERE id = " + po.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("5.2.4 更新不存在的 ID 返回 0")
    void updateUnknownIdReturnsZero() {
        assertThat(employeeMapper.updateLoginStatus(999999)).isZero();
    }

    @Test
    @DisplayName("5.2.5 [红灯] login_count 为 NULL 时仍能自增")
    void updateHandlesNullLoginCount() throws Exception {
        EmployeePO po = insert(USERNAME, 1, PASSWORD);
        execute("UPDATE tb_employee SET login_count = NULL WHERE id = " + po.getId());

        employeeMapper.updateLoginStatus(po.getId());

        assertThat(scalar("SELECT login_count FROM tb_employee WHERE id = " + po.getId())).isEqualTo(1);
    }
}
