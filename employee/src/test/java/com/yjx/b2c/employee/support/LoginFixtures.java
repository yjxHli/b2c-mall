package com.yjx.b2c.employee.support;

import cn.hutool.crypto.digest.BCrypt;
import com.yjx.b2c.common.bean.ClientInfo;
import com.yjx.b2c.employee.dao.po.EmployeePO;
import com.yjx.b2c.employee.service.event.EmployeeLoginEvent;
import com.yjx.b2c.employee.web.request.LoginRequestVO;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * 员工登录链路测试数据工厂（任务项 A.2.6 / A.2.7）。
 *
 * @author yjx
 * @date 2026/10/16
 */
public final class LoginFixtures {

    public static final String USERNAME = "admin";
    public static final String PASSWORD = "123456";
    public static final Integer SHOP_ID = 88;
    public static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/120.0.0.0 Safari/537.36";

    private LoginFixtures() {
    }

    public static LoginRequestVO loginRequest(String username, String password, Integer shopId) {
        LoginRequestVO vo = new LoginRequestVO();
        vo.setUsername(username);
        vo.setPassword(password);
        vo.setShopId(shopId);
        return vo;
    }

    /** 以 BCrypt 哈希承载明文密码的员工基线。 */
    public static EmployeePO employeePO(Integer id, Integer shopId, String username, String rawPassword, Integer status) {
        EmployeePO po = new EmployeePO();
        po.setId(id);
        po.setShopId(shopId);
        po.setUsername(username);
        po.setPassword(rawPassword == null ? null : BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        po.setAvatarUrl("/avatars/default.png");
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

    public static EmployeeLoginEvent loginEvent(Integer employeeId, Integer shopId, String username) {
        EmployeeLoginEvent event = new EmployeeLoginEvent();
        event.setEmployeeId(employeeId);
        event.setShopId(shopId);
        event.setUsername(username);
        event.setClientInfo(clientInfo());
        return event;
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
