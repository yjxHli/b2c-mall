package com.yjx.b2c.shop.dao.po;

import lombok.Data;
import java.util.Date;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 登录审计日志（tb_login_logs）
 */
@Data
public class LoginLogPO {
    private Integer id;
    private Integer userId;
    /** 1-店铺 2-员工 */
    private Integer userType;
    private String loginIp;
    private String loginDevice;
    private String loginLocation;
    private Date loginTime;
    private String userAgent;
    /** 1-成功 0-失败 */
    private Integer status;
}
