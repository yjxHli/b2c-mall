package com.yjx.b2c.shop.dao.mapper;

import com.yjx.b2c.shop.support.ShopDaoTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author yjx
 * @date 2026/10/15
 * @description
 */
class ShopMapperSmokeTest extends ShopDaoTestSupport {

    @Test
    @DisplayName("测试基座可用：能连上 SQLite 测试库并执行计数查询")
    void mapperIsUsable() {
        assertThat(shopMapper.exist("不存在")).isZero();
    }
}
