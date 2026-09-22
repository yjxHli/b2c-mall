package com.yjx.b2c.common.util;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.yjx.b2c.common.bean.ClientInfo;

import javax.servlet.http.HttpServletRequest;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 从请求中提取登录客户端信息（IP、设备、地点）
 */
public class ClientInfoUtil {
    private static final String UNKNOWN = "未知";
    private static final String[] IP_HEADERS = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};

    private ClientInfoUtil() {
    }

    public static ClientInfo of(HttpServletRequest request) {
        ClientInfo info = new ClientInfo();
        String ip = resolveIp(request);
        info.setIp(ip);
        info.setUserAgent(request.getHeader("User-Agent"));
        info.setDevice(resolveDevice(info.getUserAgent()));
        info.setLocation(resolveLocation(ip));
        return info;
    }

    private static String resolveIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                int comma = value.indexOf(',');
                return comma > 0 ? value.substring(0, comma).trim() : value.trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static String resolveDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN;
        }
        UserAgent ua = UserAgentUtil.parse(userAgent);
        if (ua == null) {
            return UNKNOWN;
        }
        String os = ua.getOs() == null ? UNKNOWN : ua.getOs().getName();
        String browser = ua.getBrowser() == null ? UNKNOWN : ua.getBrowser().getName();
        return os + "/" + browser;
    }

    /**
     * 依据 IP 粗略判断登录地点，公网 IP 的归属地需接入 IP 库/第三方服务后在此扩展
     */
    private static String resolveLocation(String ip) {
        if (ip == null || ip.isBlank()) {
            return UNKNOWN;
        }
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "localhost".equalsIgnoreCase(ip)) {
            return "本机";
        }
        return isPrivateIp(ip) ? "内网" : UNKNOWN;
    }

    private static boolean isPrivateIp(String ip) {
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("169.254.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }
}
