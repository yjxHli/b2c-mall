package com.yjx.b2c.shop.service.event.handler;

import cn.hutool.core.lang.Assert;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.yjx.b2c.shop.dao.mapper.ShopMapper;
import com.yjx.b2c.shop.service.event.ShopLoginEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 用户状态观察者：更新店铺最后登录时间与累计登录次数
 */
@Component
@Slf4j
public class ShopUserStatusEventHandle implements InitializingBean {
    @Autowired
    private EventBus eventBus;
    @Autowired
    private ShopMapper shopMapper;

    @Subscribe
    public void handler(ShopLoginEvent event) {
        int row = shopMapper.updateLoginStatus(event.getShopId());
        Assert.equals(row, 1, "更新店铺登录状态失败");
        log.info("店铺登录状态已更新, shopId={}", event.getShopId());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        eventBus.register(this);
    }
}
