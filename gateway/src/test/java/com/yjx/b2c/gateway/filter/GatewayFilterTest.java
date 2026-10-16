package com.yjx.b2c.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关过滤器对登录请求的放行与透传（F7.2）。
 *
 * @author yjx
 * @date 2026/10/16
 */
class GatewayFilterTest {

    private final LogginFilter filter = new LogginFilter();

    private static GatewayFilterChain recordingChain(AtomicReference<ServerWebExchange> seen,
                                                     AtomicBoolean called) {
        return exchange -> {
            seen.set(exchange);
            called.set(true);
            return Mono.empty();
        };
    }

    @Test
    @DisplayName("7.2.1 过滤器记录路径后放行，且不改写请求")
    void filterPassesThroughWithoutMutatingRequest() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/shop/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"adminAccount\":\"admin\"}");
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicBoolean called = new AtomicBoolean(false);
        AtomicReference<ServerWebExchange> seen = new AtomicReference<>();

        filter.filter(exchange, recordingChain(seen, called)).block();

        assertThat(called).isTrue();
        assertThat(seen.get()).isSameAs(exchange);
        assertThat(exchange.getRequest().getPath().value()).isEqualTo("/shop/login");
        assertThat(exchange.getRequest().getMethodValue()).isEqualTo("POST");
    }

    @Test
    @DisplayName("7.2.2 X-Forwarded-For 与 User-Agent 原样透传（审计依赖）")
    void forwardsAuditHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/employee/login")
                .header("X-Forwarded-For", "1.1.1.1")
                .header("User-Agent", "UA/1.0")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicBoolean called = new AtomicBoolean(false);

        filter.filter(exchange, recordingChain(new AtomicReference<>(), called)).block();

        assertThat(called).isTrue();
        assertThat(exchange.getRequest().getHeaders().getFirst("X-Forwarded-For")).isEqualTo("1.1.1.1");
        assertThat(exchange.getRequest().getHeaders().getFirst("User-Agent")).isEqualTo("UA/1.0");
    }

    @Test
    @DisplayName("7.2.3 登录路径当前无需鉴权即可通过过滤器链（现状锚点）")
    void loginRequiresNoAuthorization() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/shop/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"adminAccount\":\"admin\"}");
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicBoolean called = new AtomicBoolean(false);

        filter.filter(exchange, recordingChain(new AtomicReference<>(), called)).block();

        // 引入鉴权后，本用例必须改为断言「登录在白名单内」
        assertThat(called).isTrue();
    }
}
