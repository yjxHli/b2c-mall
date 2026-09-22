package com.yjx.b2c.shop.service.event.handler;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.yjx.b2c.shop.service.MessageService;
import com.yjx.b2c.shop.service.event.ShopRegisterEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yjx
 * @date 2026/9/11
 * @description
 */
@Slf4j
@Component
public class InitShopMessageEventHandle implements InitializingBean {
    @Autowired
    private EventBus eventBus;
    @Autowired
    private MessageService messageService;

    @Subscribe
    public void handler(ShopRegisterEvent event) {
        log.info("收到注册成功事件：{}，插入欢迎短信", event);
        messageService.saveWeclomeMessage(event.getShopId(), "欢迎注册本系统", "系统规范，系统规约");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        eventBus.register(this);
    }
}
