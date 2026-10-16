package com.yjx.b2c.employee.service.event;

import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订阅者异常统一出口：与 shop 侧对齐，保证异步事件失败可观测而不是静默丢失。
 */
@Component
@Slf4j
public class EventBusExceptionHandler implements SubscriberExceptionHandler {

    @Override
    public void handleException(Throwable exception, SubscriberExceptionContext context) {
        log.error("事件订阅者执行失败, event={}, subscriber={}",
                context.getEvent(), context.getSubscriberMethod(), exception);
    }
}
