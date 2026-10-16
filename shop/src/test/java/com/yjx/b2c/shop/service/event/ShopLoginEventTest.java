package com.yjx.b2c.shop.service.event;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.yjx.b2c.common.bean.ClientInfo;
import com.yjx.b2c.shop.support.LoginFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 登录成功事件契约（F1.5）。
 *
 * @author yjx
 * @date 2026/10/16
 */
class ShopLoginEventTest {

    @Test
    @DisplayName("1.5.1 ShopLoginEvent 携带店铺身份与客户端信息")
    void carriesShopIdentity() {
        ShopLoginEvent event = new ShopLoginEvent();
        event.setShopId(1);
        event.setShopName("测试店铺");
        event.setAdminAccount("admin");
        event.setClientInfo(LoginFixtures.clientInfo());

        assertThat(event.getShopId()).isEqualTo(1);
        assertThat(event.getShopName()).isEqualTo("测试店铺");
        assertThat(event.getAdminAccount()).isEqualTo("admin");
        assertThat(event.getClientInfo()).isInstanceOf(ClientInfo.class);
    }

    @Test
    @DisplayName("1.5.3 登录事件不会触发注册事件的订阅者")
    void loginEventDoesNotReachRegisterSubscribers() {
        AtomicReference<Object> seen = new AtomicReference<>();
        EventBus bus = new EventBus();
        bus.register(new Object() {
            @Subscribe
            public void on(ShopRegisterEvent event) {
                seen.set(event);
            }
        });

        bus.post(new ShopLoginEvent());

        assertThat(seen.get()).isNull();
    }
}
