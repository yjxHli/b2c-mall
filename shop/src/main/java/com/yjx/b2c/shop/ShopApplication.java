package com.yjx.b2c.shop;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executors;

/**
 * @author yjx
 * @date 2026/9/10
 * @description ${DESCRIPTION}
 */

@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients
public class ShopApplication {

    @Bean
    public EventBus eventBus() {
        return new AsyncEventBus(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }

    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }
}