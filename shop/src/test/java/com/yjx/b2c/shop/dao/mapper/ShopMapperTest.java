package com.yjx.b2c.shop.dao.mapper;

import com.yjx.b2c.shop.dao.po.ShopPO;
import com.yjx.b2c.shop.support.ShopDaoTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShopMapperTest extends ShopDaoTestSupport {

    private ShopPO po(String shopName, String account) {
        ShopPO po = new ShopPO();
        po.setShopName(shopName);
        po.setAdminAccount(account);
        po.setAdminPassword("123456");
        po.setStatus(1);
        return po;
    }

    @Test
    @DisplayName("1.4.1/1.4.2 插入 tb_shop 并回填自增主键")
    void insertAndBackfillId() {
        ShopPO po = po("测试店铺", "admin");

        assertThat(shopMapper.registerShop(po)).isEqualTo(1);
        assertThat(po.getId()).isNotNull().isPositive();

        ShopPO saved = shopMapper.findByAccount("admin");
        assertThat(saved.getShopName()).isEqualTo("测试店铺");
        assertThat(saved.getAdminPassword()).isEqualTo("123456");
        assertThat(saved.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("1.4.4 created_at / updated_at 由数据库默认值兜底")
    void timestampsHaveDefaults() throws Exception {
        shopMapper.registerShop(po("测试店铺", "admin"));

        try (Statement statement = session.getConnection().createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT created_at, updated_at FROM tb_shop WHERE shop_name = '测试店铺'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("created_at")).isNotBlank();
            assertThat(rs.getString("updated_at")).isNotBlank();
        }
    }

    @Test
    @DisplayName("1.4.3 shop_name 为 NULL 时插入必须失败")
    void shopNameIsNotNull() {
        ShopPO po = po("测试店铺", "admin");
        po.setShopName(null);

        assertThatThrownBy(() -> shopMapper.registerShop(po)).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("1.4.5 管理员账号索引存在（登录链路回归保护）")
    void adminAccountIndexExists() throws Exception {
        try (Statement statement = session.getConnection().createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type = 'index' "
                             + "AND name = 'idx_shop_admin_account'")) {
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    @DisplayName("3.2.1 exist 查询语义正确")
    void existCountsExactly() {
        assertThat(shopMapper.exist("测试店铺")).isZero();
        shopMapper.registerShop(po("测试店铺", "admin"));
        assertThat(shopMapper.exist("测试店铺")).isEqualTo(1);
        assertThat(shopMapper.exist("别的店铺")).isZero();
    }

    @Test
    @DisplayName("3.2.2 大小写敏感语义固化：ShopA 与 shopa 视为不同店铺")
    void shopNameComparisonIsCaseSensitive() {
        shopMapper.registerShop(po("ShopA", "admin1"));

        assertThat(shopMapper.exist("shopa")).isZero();
    }

    @Test
    @DisplayName("3.3.2 [红灯] 同名店铺只能写入一条")
    void duplicateShopNameRejectedByDatabase() {
        shopMapper.registerShop(po("测试店铺", "admin1"));

        assertThatThrownBy(() -> shopMapper.registerShop(po("测试店铺", "admin2")))
                .hasMessageContaining("UNIQUE constraint failed");
    }
}