package com.yjx.b2c.shop.service.event;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订阅者异常日志测试（任务项 6.4.2）。
 */
class EventBusExceptionHandlerTest {

    @Test
    @DisplayName("6.4.2 订阅者异常会写出包含事件信息的 ERROR 日志")
    void handlerWritesErrorLog() {
        Logger logger = (Logger) LoggerFactory.getLogger(EventBusExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        EventBus eventBus = new AsyncEventBus(Runnable::run, new EventBusExceptionHandler());
        eventBus.register(new Object() {
            @Subscribe
            public void on(ShopRegisterEvent event) {
                throw new IllegalStateException("boom");
            }
        });

        eventBus.post(new ShopRegisterEvent());
        logger.detachAppender(appender);

        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list.get(0).getFormattedMessage()).contains("事件订阅者执行失败");
    }
}
