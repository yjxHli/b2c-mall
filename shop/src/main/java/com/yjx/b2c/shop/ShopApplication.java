package com.yjx.b2c.shop;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executors;

/**
 * @author yjx
 * @date 2026/9/10
 * @description ${DESCRIPTION}
 */

@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class ShopApplication {

    @Bean
    public EventBus eventBus(SubscriberExceptionHandler subscriberExceptionHandler) {
        return new AsyncEventBus(
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
                subscriberExceptionHandler);
    }

    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }
}