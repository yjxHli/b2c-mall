package com.yjx.b2c.shop.dao.mapper;

import com.yjx.b2c.shop.dao.po.ShopPO;
import com.yjx.b2c.shop.support.LoginFixtures;
import com.yjx.b2c.shop.support.ShopDaoTestSupport;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

import static com.yjx.b2c.shop.support.LoginFixtures.ACCOUNT;
import static com.yjx.b2c.shop.support.LoginFixtures.PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 店铺账号查询与登录状态更新的 SQL 语义（F1.4 / F3.2 / F5.2 / F6.1）。
 *
 * @author yjx
 * @date 2026/10/16
 */
class ShopLoginMapperTest extends ShopDaoTestSupport {

    private int seq;

    private ShopPO insertShop(String account, String rawPassword, Integer status) {
        ShopPO po = LoginFixtures.shopPO(null, account, rawPassword, status);
        po.setShopName("店铺" + (++seq) + "-" + account);
        shopMapper.registerShop(po);
        return po;
    }

    private Object scalar(String sql) throws Exception {
        try (Statement statement = session.getConnection().createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getObject(1) : null;
        }
    }

    private String queryPlan(String sql) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (Statement statement = session.getConnection().createStatement();
             ResultSet rs = statement.executeQuery("EXPLAIN QUERY PLAN " + sql)) {
            while (rs.next()) {
                sb.append(rs.getString("detail")).append(';');
            }
        }
        return sb.toString();
    }

    @Test
    @DisplayName("1.4.1 findByAccount 按账号精确等值查询")
    void findByAccountMatchesExactly() {
        insertShop("admin", PASSWORD, 1);
        insertShop("admin2", PASSWORD, 1);

        ShopPO found = shopMapper.findByAccount("admin");
        assertThat(found).isNotNull();
        assertThat(found.getAdminAccount()).isEqualTo("admin");
    }

    @Test
    @DisplayName("1.4.3 查询结果字段完整（含 status）")
    void mapsAllColumns() {
        ShopPO inserted = insertShop("admin", PASSWORD, 0);

        ShopPO found = shopMapper.findByAccount("admin");
        assertThat(found.getId()).isEqualTo(inserted.getId());
        assertThat(found.getShopName()).isEqualTo(inserted.getShopName());
        assertThat(found.getAdminAccount()).isEqualTo("admin");
        assertThat(found.getAdminPassword()).startsWith("$2");
        assertThat(found.getStatus()).isZero();
    }

    @Test
    @DisplayName("1.4.4/2.3.1 查询不到的账号返回 null 且不抛异常")
    void missingAccountReturnsNull() {
        assertThat(shopMapper.findByAccount("nobody")).isNull();
    }

    @Test
    @DisplayName("1.4.5 账号比较大小写敏感")
    void accountLookupIsCaseSensitive() {
        insertShop("Admin", PASSWORD, 1);

        assertThat(shopMapper.findByAccount("admin")).isNull();
        assertThat(shopMapper.findByAccount("Admin")).isNotNull();
    }

    @Test
    @DisplayName("1.4.6 ShopPO 未映射登录状态字段（现状锚点）")
    void shopPoHasNoLoginStatusMapping() {
        String[] names = Arrays.stream(ShopPO.class.getDeclaredFields()).map(Field::getName).toArray(String[]::new);
        assertThat(names).doesNotContain("lastLoginTime", "loginCount");
    }

    @Test
    @DisplayName("1.4.7 账号查询命中 idx_shop_admin_account")
    void accountLookupUsesIndex() throws Exception {
        insertShop("admin", PASSWORD, 1);

        // 加了 uk_shop_admin_account 之后，SQLite 会优先选唯一索引；断言命中账号索引且不是全表扫描
        String plan = queryPlan("SELECT id FROM tb_shop WHERE admin_account = 'admin'");
        assertThat(plan).containsAnyOf("idx_shop_admin_account", "uk_shop_admin_account");
        assertThat(plan).doesNotContain("SCAN tb_shop");
    }

    @Test
    @DisplayName("3.2.1 查询返回 status 列")
    void returnsStatusColumn() {
        insertShop("enabled", PASSWORD, 1);
        insertShop("disabled", PASSWORD, 0);

        assertThat(shopMapper.findByAccount("enabled").getStatus()).isEqualTo(1);
        assertThat(shopMapper.findByAccount("disabled").getStatus()).isZero();
    }

    @Test
    @DisplayName("3.2.2 建表默认 status=1")
    void defaultStatusIsOne() throws Exception {
        try (Statement statement = session.getConnection().createStatement()) {
            statement.executeUpdate("INSERT INTO tb_shop (shop_name, admin_account, admin_password) "
                    + "VALUES ('默认状态店铺', 'default-status', 'x')");
        }

        assertThat(scalar("SELECT status FROM tb_shop WHERE admin_account = 'default-status'")).isEqualTo(1);
    }

    @Test
    @DisplayName("5.2.1/5.2.2/5.2.3 登录状态更新：计数自增、时间刷新")
    void updateLoginStatusIncrementsAndStamps() throws Exception {
        ShopPO po = insertShop("admin", PASSWORD, 1);
        assertThat(scalar("SELECT login_count FROM tb_shop WHERE id = " + po.getId())).isEqualTo(0);

        assertThat(shopMapper.updateLoginStatus(po.getId())).isEqualTo(1);
        assertThat(scalar("SELECT login_count FROM tb_shop WHERE id = " + po.getId())).isEqualTo(1);
        assertThat(scalar("SELECT last_login_time FROM tb_shop WHERE id = " + po.getId())).isNotNull();

        assertThat(shopMapper.updateLoginStatus(po.getId())).isEqualTo(1);
        assertThat(scalar("SELECT login_count FROM tb_shop WHERE id = " + po.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("5.2.3 updated_at 同步刷新")
    void updateLoginStatusRefreshesUpdatedAt() throws Exception {
        ShopPO po = insertShop("admin", PASSWORD, 1);

        shopMapper.updateLoginStatus(po.getId());

        Object updatedAt = scalar("SELECT updated_at FROM tb_shop WHERE id = " + po.getId());
        assertThat(updatedAt).isNotNull();
    }

    @Test
    @DisplayName("5.2.4 更新不存在的 ID 返回 0")
    void updateUnknownIdReturnsZero() {
        assertThat(shopMapper.updateLoginStatus(999999)).isZero();
    }

    @Test
    @DisplayName("5.2.5 [红灯] login_count 为 NULL 时仍能自增")
    void updateLoginStatusHandlesNullLoginCount() throws Exception {
        ShopPO po = insertShop("admin", PASSWORD, 1);
        try (Statement statement = session.getConnection().createStatement()) {
            statement.executeUpdate("UPDATE tb_shop SET login_count = NULL WHERE id = " + po.getId());
        }

        shopMapper.updateLoginStatus(po.getId());

        assertThat(scalar("SELECT login_count FROM tb_shop WHERE id = " + po.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("6.3.1 [红灯] admin_account 唯一索引存在")
    void adminAccountUniqueIndexExists() throws Exception {
        assertThat(scalar("SELECT count(1) FROM sqlite_master WHERE type = 'index' "
                + "AND name = 'uk_shop_admin_account'")).isEqualTo(1);
    }

    @Test
    @DisplayName("6.1.2/6.3.3 [红灯] 同一 admin_account 只能存在一条店铺")
    void duplicateAdminAccountRejected() {
        insertShop(ACCOUNT, PASSWORD, 1);

        assertThatThrownBy(() -> insertShop(ACCOUNT, PASSWORD, 1))
                .isInstanceOf(PersistenceException.class)
                .hasMessageContaining("UNIQUE constraint failed");
    }

    @Test
    @DisplayName("6.3.2 uk_shop_shop_name 唯一索引仍是回归保护")
    void shopNameUniqueIndexExists() throws Exception {
        assertThat(scalar("SELECT count(1) FROM sqlite_master WHERE type = 'index' "
                + "AND name = 'uk_shop_shop_name'")).isEqualTo(1);
    }
}
