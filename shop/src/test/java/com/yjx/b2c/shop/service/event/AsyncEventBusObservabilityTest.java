package com.yjx.b2c.shop.service.event;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异步事件可观测性测试（任务项 6.2.1）。
 */
class AsyncEventBusObservabilityTest {

    @Test
    @DisplayName("6.2.1 订阅者异常被 SubscriberExceptionHandler 捕获，而不是静默丢失")
    void subscriberExceptionIsReported() {
        List<Throwable> captured = new CopyOnWriteArrayList<>();
        EventBus eventBus = new AsyncEventBus(Runnable::run,
                (exception, context) -> captured.add(exception));

        eventBus.register(new Object() {
            @Subscribe
            public void on(ShopRegisterEvent event) {
                throw new IllegalStateException("employee service down");
            }
        });

        eventBus.post(new ShopRegisterEvent());

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0)).isInstanceOf(IllegalStateException.class);
    }
}
