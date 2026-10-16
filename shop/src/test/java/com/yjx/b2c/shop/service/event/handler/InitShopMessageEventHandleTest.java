package com.yjx.b2c.shop.service.event.handler;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.shop.service.MessageService;
import com.yjx.b2c.shop.service.event.ShopRegisterEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 欢迎站内信订阅者测试（任务项 5.1.x）。
 */
class InitShopMessageEventHandleTest {

    private EventBus eventBus;
    private MessageService messageService;
    private InitShopMessageEventHandle handle;

    @BeforeEach
    void setUp() throws Exception {
        eventBus = new EventBus();
        messageService = mock(MessageService.class);

        handle = new InitShopMessageEventHandle();
        ReflectionTestUtils.setField(handle, "eventBus", eventBus);
        ReflectionTestUtils.setField(handle, "messageService", messageService);
        handle.afterPropertiesSet();
    }

    private ShopRegisterEvent event() {
        ShopRegisterEvent event = new ShopRegisterEvent();
        event.setShopId(88);
        event.setShopName("测试店铺");
        return event;
    }

    @Test
    @DisplayName("5.1.1/5.1.2 收到事件后写入欢迎站内信")
    void savesWelcomeMessage() {
        eventBus.post(event());

        verify(messageService).saveWeclomeMessage(88, "欢迎注册本系统", "系统规范，系统规约");
    }

    @Test
    @DisplayName("5.1.3 站内信写入失败不应影响注册主流程")
    void messageFailureIsIsolated() {
        doThrow(new IllegalStateException("db down"))
                .when(messageService).saveWeclomeMessage(88, "欢迎注册本系统", "系统规范，系统规约");

        assertThatCode(() -> eventBus.post(event())).doesNotThrowAnyException();
    }
}
