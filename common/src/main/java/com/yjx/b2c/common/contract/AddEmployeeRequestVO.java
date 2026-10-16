package com.yjx.b2c.common.contract;

import lombok.Data;

/**
 * 新增员工账号的跨服务契约（shop -> employee）。
 * 收敛到 common 模块，避免两侧各自定义导致字段漂移。
 */
@Data
public class AddEmployeeRequestVO {

    private Integer shopId;
    private String username;
    private String password;
}
