package com.yjx.b2c.employee;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.eventbus.SubscriberExceptionHandler;
import com.yjx.b2c.employee.service.event.EmployeeLoginEvent;
import com.yjx.b2c.employee.support.LoginFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 员工侧事件总线装配（F4.1.8）：与 shop 侧对齐，订阅者异常必须可观测。
 *
 * @author yjx
 * @date 2026/10/16
 */
class EmployeeEventBusConfigTest {

    @Test
    @DisplayName("4.1.8 [红灯] EventBus Bean 装配了订阅者异常处理器")
    void eventBusIsWiredWithExceptionHandler() {
        Method eventBusMethod = Arrays.stream(EmployeeApplication.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Bean.class) && EventBus.class.equals(m.getReturnType()))
                .findFirst()
                .orElse(null);

        assertThat(eventBusMethod).as("EmployeeApplication 必须声明 EventBus Bean").isNotNull();
        assertThat(eventBusMethod.getParameterTypes())
                .as("EventBus 必须注入 SubscriberExceptionHandler")
                .contains(SubscriberExceptionHandler.class);

        Class<?> handlerClass;
        try {
            handlerClass = Class.forName("com.yjx.b2c.employee.service.event.EventBusExceptionHandler");
        } catch (ClassNotFoundException ex) {
            throw new AssertionError("必须提供 SubscriberExceptionHandler 实现", ex);
        }
        assertThat(SubscriberExceptionHandler.class).isAssignableFrom(handlerClass);
    }

    @Test
    @DisplayName("4.1.8 装配处理器后，订阅者异常被处理器接住")
    void handlerMakesSubscriberExceptionObservable() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> captured = new AtomicReference<>();
        SubscriberExceptionHandler handler = (exception, context) -> {
            captured.set(exception);
            latch.countDown();
        };

        Executor executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        EventBus bus = new AsyncEventBus(executor, handler);
        bus.register(new Object() {
            @Subscribe
            public void on(EmployeeLoginEvent event) {
                throw new IllegalStateException("audit down");
            }
        });

        bus.post(LoginFixtures.loginEvent(33, 88, "admin"));

        assertThat(latch.await(5, TimeUnit.SECONDS)).as("异常处理器应被调用").isTrue();
        assertThat(captured.get()).isInstanceOf(IllegalStateException.class);
    }
}
