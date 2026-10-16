package com.yjx.b2c.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关路由配置回归测试（任务项 7.1.x）。
 */
class GatewayRouteConfigTest {

    private static final int MAX_ROUTES = 20;

    private PropertySource<?> applicationConfig() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"));
        return sources.get(0);
    }

    /** 找到声明了该 Path 断言的路由目标地址 */
    private Optional<String> targetOf(PropertySource<?> source, String path) {
        for (int i = 0; i < MAX_ROUTES; i++) {
            Object predicate = source.getProperty("spring.cloud.gateway.routes[" + i + "].predicates[0]");
            if (predicate == null) {
                return Optional.empty();
            }
            if (("Path=" + path).equals(String.valueOf(predicate))) {
                return Optional.of(String.valueOf(
                        source.getProperty("spring.cloud.gateway.routes[" + i + "].uri")));
            }
        }
        return Optional.empty();
    }

    @Test
    @DisplayName("7.1.1 /shop/** 路由到 lb://shop-service")
    void shopRouteTargetsShopService() throws Exception {
        assertThat(targetOf(applicationConfig(), "/shop/**")).contains("lb://shop-service");
    }

    @Test
    @DisplayName("7.1.2 /employee/** 路由到 lb://employee-service")
    void employeeRouteTargetsEmployeeService() throws Exception {
        assertThat(targetOf(applicationConfig(), "/employee/**")).contains("lb://employee-service");
    }

    @Test
    @DisplayName("7.1.3 /shop/login 与 /employee/login 各只被一条路由匹配")
    void loginPathsAreNotShadowed() throws Exception {
        PropertySource<?> source = applicationConfig();

        assertThat(countPatternsMatching(source, "/shop/login")).isEqualTo(1);
        assertThat(countPatternsMatching(source, "/employee/login")).isEqualTo(1);
    }

    /** 统计有多少条 Path 断言能覆盖该路径（防止新增路由抢占登录入口） */
    private int countPatternsMatching(PropertySource<?> source, String path) {
        int matches = 0;
        for (int i = 0; i < MAX_ROUTES; i++) {
            Object predicate = source.getProperty("spring.cloud.gateway.routes[" + i + "].predicates[0]");
            if (predicate == null) {
                break;
            }
            String value = String.valueOf(predicate);
            if (!value.startsWith("Path=")) {
                continue;
            }
            String pattern = value.substring("Path=".length());
            String prefix = pattern.endsWith("/**") ? pattern.substring(0, pattern.length() - 3) : pattern;
            if (path.equals(pattern) || path.startsWith(prefix + "/")) {
                matches++;
            }
        }
        return matches;
    }

    @Test
    @DisplayName("7.1.2 所有路由都必须走服务发现，禁止写死 IP")
    void allRoutesUseServiceDiscovery() throws Exception {
        PropertySource<?> source = applicationConfig();
        for (int i = 0; i < MAX_ROUTES; i++) {
            Object uri = source.getProperty("spring.cloud.gateway.routes[" + i + "].uri");
            if (uri == null) {
                break;
            }
            assertThat(String.valueOf(uri)).startsWith("lb://");
        }
    }
}
