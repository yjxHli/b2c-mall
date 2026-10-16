package com.yjx.b2c.shop.service.event.handler;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.shop.dao.mapper.ShopMapper;
import com.yjx.b2c.shop.service.event.ShopLoginEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 店铺登录状态处理器（F5.1）。
 *
 * @author yjx
 * @date 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
class ShopUserStatusEventHandleTest {

    @Mock
    private EventBus eventBus;
    @Mock
    private ShopMapper shopMapper;
    @InjectMocks
    private ShopUserStatusEventHandle handler;

    private static ShopLoginEvent event(Integer shopId) {
        ShopLoginEvent event = new ShopLoginEvent();
        event.setShopId(shopId);
        return event;
    }

    @Test
    @DisplayName("5.1.1 处理器随容器注册到 EventBus")
    void registersWithEventBus() throws Exception {
        handler.afterPropertiesSet();

        verify(eventBus).register(handler);
    }

    @Test
    @DisplayName("5.1.2 用事件中的 shopId 更新")
    void updatesUsingShopId() {
        when(shopMapper.updateLoginStatus(88)).thenReturn(1);

        handler.handler(event(88));

        verify(shopMapper).updateLoginStatus(88);
    }

    @Test
    @DisplayName("5.1.3 更新行数必须为 1，否则抛异常")
    void rowCountMustBeOne() {
        when(shopMapper.updateLoginStatus(88)).thenReturn(1);
        assertThatCode(() -> handler.handler(event(88))).doesNotThrowAnyException();

        when(shopMapper.updateLoginStatus(88)).thenReturn(0);
        assertThatThrownBy(() -> handler.handler(event(88))).hasMessage("更新店铺登录状态失败");
    }

    @Test
    @DisplayName("5.1.5 状态处理器不依赖 clientInfo")
    void doesNotDependOnClientInfo() {
        when(shopMapper.updateLoginStatus(88)).thenReturn(1);
        ShopLoginEvent event = event(88);
        event.setClientInfo(null);

        assertThatCode(() -> handler.handler(event)).doesNotThrowAnyException();
        verify(shopMapper).updateLoginStatus(88);
    }
}
