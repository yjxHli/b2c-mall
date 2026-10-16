package com.yjx.b2c.shop.service.event.handler;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.yjx.b2c.shop.dao.mapper.LoginLogMapper;
import com.yjx.b2c.shop.dao.po.LoginLogPO;
import com.yjx.b2c.shop.service.event.ShopLoginEvent;
import com.yjx.b2c.shop.support.LoginFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 店铺登录审计处理器（F4.1）。
 *
 * @author yjx
 * @date 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
class ShopAuditLogEventHandleTest {

    @Mock
    private EventBus eventBus;
    @Mock
    private LoginLogMapper loginLogMapper;
    @InjectMocks
    private ShopAuditLogEventHandle handler;

    private static ShopLoginEvent event() {
        ShopLoginEvent event = new ShopLoginEvent();
        event.setShopId(88);
        event.setShopName("测试店铺");
        event.setAdminAccount("admin");
        event.setClientInfo(LoginFixtures.clientInfo());
        return event;
    }

    @Test
    @DisplayName("4.1.1 处理器随容器启动注册到 EventBus")
    void registersWithEventBus() throws Exception {
        handler.afterPropertiesSet();

        verify(eventBus).register(handler);
    }

    @Test
    @DisplayName("4.1.2/4.1.3/4.1.4 事件映射为审计记录：主体、类型、客户端信息、成功状态")
    void mapsEventToLoginLog() {
        when(loginLogMapper.saveLoginLog(any(LoginLogPO.class))).thenReturn(1);

        handler.handler(event());

        ArgumentCaptor<LoginLogPO> captor = ArgumentCaptor.forClass(LoginLogPO.class);
        verify(loginLogMapper).saveLoginLog(captor.capture());
        LoginLogPO po = captor.getValue();
        assertThat(po.getUserId()).isEqualTo(88);
        assertThat(po.getUserType()).isEqualTo(1);
        assertThat(po.getLoginIp()).isEqualTo("1.1.1.1");
        assertThat(po.getLoginDevice()).isEqualTo("Windows 10/Chrome");
        assertThat(po.getLoginLocation()).isEqualTo("未知");
        assertThat(po.getUserAgent()).isEqualTo(LoginFixtures.USER_AGENT);
        assertThat(po.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("4.1.5 落库行数必须为 1，否则抛异常")
    void rowCountMustBeOne() {
        when(loginLogMapper.saveLoginLog(any(LoginLogPO.class))).thenReturn(1);
        assertThatCode(() -> handler.handler(event())).doesNotThrowAnyException();

        when(loginLogMapper.saveLoginLog(any(LoginLogPO.class))).thenReturn(0);
        assertThatThrownBy(() -> handler.handler(event())).hasMessage("保存登录审计日志失败");
    }

    @Test
    @DisplayName("4.1.6 [红灯] clientInfo 为 null 时不得 NPE")
    void nullClientInfoDoesNotThrow() {
        ShopLoginEvent event = event();
        event.setClientInfo(null);

        assertThatCode(() -> handler.handler(event)).doesNotThrowAnyException();
        verify(loginLogMapper, never()).saveLoginLog(any(LoginLogPO.class));
    }

    @Test
    @DisplayName("4.1.7/4.1.9 订阅者异常被异常处理器接住，不外泄到发布方")
    void subscriberExceptionIsObservableAndIsolated() {
        AtomicReference<Throwable> captured = new AtomicReference<>();
        EventBus bus = new EventBus((exception, context) -> captured.set(exception));
        bus.register(new Object() {
            @Subscribe
            public void on(ShopLoginEvent event) {
                throw new IllegalStateException("audit down");
            }
        });

        assertThatCode(() -> bus.post(event())).doesNotThrowAnyException();
        assertThat(captured.get()).isInstanceOf(IllegalStateException.class).hasMessage("audit down");
    }
}
