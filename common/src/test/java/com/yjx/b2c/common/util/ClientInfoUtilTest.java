package com.yjx.b2c.common.util;

import com.yjx.b2c.common.bean.ClientInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 客户端信息提取（F4.3）。
 *
 * @author yjx
 * @date 2026/10/16
 */
class ClientInfoUtilTest {

    private static final String CHROME_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/120.0.0.0 Safari/537.36";

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.9");
        return request;
    }

    @Test
    @DisplayName("4.3.1 只取 X-Forwarded-For 的第一个 IP 并去除空白")
    void xffTakesFirstIp() {
        MockHttpServletRequest request = request();
        request.addHeader("X-Forwarded-For", "  1.1.1.1 , 2.2.2.2 ");

        assertThat(ClientInfoUtil.of(request).getIp()).isEqualTo("1.1.1.1");
    }

    @Test
    @DisplayName("4.3.2 X-Forwarded-For 为 unknown 时回退到下一个头")
    void unknownXffFallsBackToNextHeader() {
        MockHttpServletRequest request = request();
        request.addHeader("X-Forwarded-For", "unknown");
        request.addHeader("X-Real-IP", "3.3.3.3");

        assertThat(ClientInfoUtil.of(request).getIp()).isEqualTo("3.3.3.3");
    }

    @Test
    @DisplayName("4.3.2/4.3.3 无可用代理头时使用 remoteAddr")
    void fallsBackToRemoteAddr() {
        MockHttpServletRequest request = request();
        request.addHeader("X-Forwarded-For", "unknown");
        request.addHeader("Proxy-Client-IP", "");
        request.setRemoteAddr("10.1.2.3");

        assertThat(ClientInfoUtil.of(request).getIp()).isEqualTo("10.1.2.3");
    }

    @Test
    @DisplayName("4.3.4 User-Agent 被解析为 操作系统/浏览器")
    void userAgentParsedToDevice() {
        MockHttpServletRequest request = request();
        request.addHeader("User-Agent", CHROME_WINDOWS);

        String device = ClientInfoUtil.of(request).getDevice();
        assertThat(device).isNotBlank().contains("/").containsIgnoringCase("windows");
    }

    @Test
    @DisplayName("4.3.5 User-Agent 缺失或不可解析时设备为未知")
    void missingUserAgentIsUnknown() {
        MockHttpServletRequest noUa = request();
        assertThat(ClientInfoUtil.of(noUa).getDevice()).isEqualTo("未知");

        MockHttpServletRequest blank = request();
        blank.addHeader("User-Agent", "");
        assertThat(ClientInfoUtil.of(blank).getDevice()).isEqualTo("未知");

        MockHttpServletRequest garbage = request();
        garbage.addHeader("User-Agent", "\u0001\u0002");
        assertThat(ClientInfoUtil.of(garbage).getDevice()).isEqualTo("未知");
    }

    @Test
    @DisplayName("4.3.6 登录地点按 IP 归类为本机/内网/未知")
    void locationClassification() {
        assertThat(locationOf("127.0.0.1")).isEqualTo("本机");
        assertThat(locationOf("::1")).isEqualTo("本机");
        assertThat(locationOf("10.1.2.3")).isEqualTo("内网");
        assertThat(locationOf("192.168.1.7")).isEqualTo("内网");
        assertThat(locationOf("169.254.1.1")).isEqualTo("内网");
        assertThat(locationOf("172.16.0.1")).isEqualTo("内网");
        assertThat(locationOf("172.31.255.255")).isEqualTo("内网");
        assertThat(locationOf("172.15.0.1")).isEqualTo("未知");
        assertThat(locationOf("172.32.0.1")).isEqualTo("未知");
        assertThat(locationOf("8.8.8.8")).isEqualTo("未知");
    }

    private static String locationOf(String ip) {
        MockHttpServletRequest request = request();
        request.addHeader("X-Forwarded-For", ip);
        return ClientInfoUtil.of(request).getLocation();
    }
}
