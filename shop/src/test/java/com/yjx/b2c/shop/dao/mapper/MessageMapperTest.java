package com.yjx.b2c.shop.dao.mapper;

import com.yjx.b2c.shop.dao.po.MessagePO;
import com.yjx.b2c.shop.support.ShopDaoTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 站内信持久化测试（任务项 5.3.x）。
 */
class MessageMapperTest extends ShopDaoTestSupport {

    @Test
    @DisplayName("5.3.1 站内信落库列与默认值正确")
    void insertMessageColumns() throws Exception {
        MessagePO po = new MessagePO();
        po.setShopId(88);
        po.setTitle("欢迎注册本系统");
        po.setContent("系统规范，系统规约");
        po.setMsgType(1);
        po.setIsRead(0);

        assertThat(messageMapper.saveMessage(po)).isEqualTo(1);
        assertThat(po.getId()).isNotNull();

        try (Statement statement = session.getConnection().createStatement();
             ResultSet rs = statement.executeQuery("SELECT shop_id, msg_type, is_read FROM tb_messages")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("shop_id")).isEqualTo(88);
            assertThat(rs.getInt("msg_type")).isEqualTo(1);
            assertThat(rs.getInt("is_read")).isZero();
        }
    }

    @Test
    @DisplayName("5.3.2 shop_id 索引存在")
    void shopIdIndexExists() throws Exception {
        try (Statement statement = session.getConnection().createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'idx_messages_shop_id'")) {
            assertThat(rs.next()).isTrue();
        }
    }
}
