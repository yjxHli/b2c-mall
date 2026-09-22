package com.yjx.b2c.common.bean;

import lombok.Data;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 登录客户端信息：IP、设备、地点
 */
@Data
public class ClientInfo {
    /** 登录IP */
    private String ip;
    /** 登录设备（操作系统/浏览器） */
    private String device;
    /** 登录地点 */
    private String location;
    /** 浏览器 UA */
    private String userAgent;
}
