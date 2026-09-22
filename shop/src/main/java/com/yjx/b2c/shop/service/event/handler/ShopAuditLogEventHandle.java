package com.yjx.b2c.shop.service.event.handler;

import cn.hutool.core.lang.Assert;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.yjx.b2c.shop.dao.mapper.LoginLogMapper;
import com.yjx.b2c.shop.dao.po.LoginLogPO;
import com.yjx.b2c.shop.service.event.ShopLoginEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 审计日志观察者：记录店铺登录的 IP、设备、地点、时间
 */
@Component
@Slf4j
public class ShopAuditLogEventHandle implements InitializingBean {
    @Autowired
    private EventBus eventBus;
    @Autowired
    private LoginLogMapper loginLogMapper;

    @Subscribe
    public void handler(ShopLoginEvent event) {
        LoginLogPO po = new LoginLogPO();
        po.setUserId(event.getShopId());
        // 1-店铺
        po.setUserType(1);
        po.setLoginIp(event.getClientInfo().getIp());
        po.setLoginDevice(event.getClientInfo().getDevice());
        po.setLoginLocation(event.getClientInfo().getLocation());
        po.setUserAgent(event.getClientInfo().getUserAgent());
        po.setStatus(1);

        Integer row = loginLogMapper.saveLoginLog(po);
        Assert.equals(row, 1, "保存登录审计日志失败");
        log.info("店铺登录审计日志已记录, shopId={}, ip={}, device={}, location={}", event.getShopId(),
                po.getLoginIp(), po.getLoginDevice(), po.getLoginLocation());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        eventBus.register(this);
    }
}
