package com.yjx.b2c.shop.support;

import cn.hutool.crypto.digest.BCrypt;
import com.yjx.b2c.common.bean.ClientInfo;
import com.yjx.b2c.shop.dao.po.ShopPO;
import com.yjx.b2c.shop.web.request.ShopLoginRequestVO;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * 登录链路测试数据工厂（任务项 A.2.6 / A.2.7）。
 *
 * <p>集中提供客户端信息、请求构造与账号基线数据，避免每个用例重复手写。</p>
 *
 * @author yjx
 * @date 2026/10/16
 */
public final class LoginFixtures {

    public static final String ACCOUNT = "admin";
    public static final String PASSWORD = "123456";
    public static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/120.0.0.0 Safari/537.36";

    private LoginFixtures() {
    }

    public static ShopLoginRequestVO loginRequest(String account, String password) {
        ShopLoginRequestVO vo = new ShopLoginRequestVO();
        vo.setAdminAccount(account);
        vo.setAdminPassword(password);
        return vo;
    }

    /** 以 BCrypt 哈希承载明文密码的账号基线。 */
    public static ShopPO shopPO(Integer id, String account, String rawPassword, Integer status) {
        ShopPO po = new ShopPO();
        po.setId(id);
        po.setShopName("测试店铺" + (id == null ? "" : id));
        po.setAdminAccount(account);
        po.setAdminPassword(rawPassword == null ? null : BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        po.setStatus(status);
        return po;
    }

    public static ClientInfo clientInfo() {
        ClientInfo info = new ClientInfo();
        info.setIp("1.1.1.1");
        info.setDevice("Windows 10/Chrome");
        info.setLocation("未知");
        info.setUserAgent(USER_AGENT);
        return info;
    }

    public static MockHttpServletRequest mockRequest(String forwardedFor, String userAgent) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.9");
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        if (userAgent != null) {
            request.addHeader("User-Agent", userAgent);
        }
        return request;
    }
}
