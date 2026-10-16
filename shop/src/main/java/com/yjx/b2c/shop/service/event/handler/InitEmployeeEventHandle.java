package com.yjx.b2c.shop.service.event.handler;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.yjx.b2c.common.response.BaseResponseVO;
import com.yjx.b2c.shop.dao.po.OutboxPO;
import com.yjx.b2c.shop.feign.EmployeeFeignClient;
import com.yjx.b2c.common.contract.AddEmployeeRequestVO;
import com.yjx.b2c.shop.service.OutboxService;
import com.yjx.b2c.shop.service.event.ShopRegisterEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 注册成功事件订阅者：调用 employee service 初始化管理员账号。
 * 先写本地消息表再调用，失败留待重试；幂等由 employee 侧保证。
 */
@Component
@Slf4j
public class InitEmployeeEventHandle implements InitializingBean {

    public static final String EVENT_TYPE = "EMPLOYEE_INIT";

    @Autowired
    private EventBus eventBus;
    @Autowired
    private EmployeeFeignClient employeeFeignClient;
    @Autowired
    private OutboxService outboxService;

    @Subscribe
    public void handler(ShopRegisterEvent event) {
        log.info("收到注册成功事件:{}，调用employee service 初始化账户", event);

        AddEmployeeRequestVO req = new AddEmployeeRequestVO();
        req.setShopId(event.getShopId());
        req.setUsername(event.getAdminAccount());
        req.setPassword(event.getAdminPwd());

        // 先入队再调用：调用失败或进程中断时仍可补偿
        OutboxPO outbox = outboxService.enqueue(EVENT_TYPE, event.getShopId(), req);
        try {
            BaseResponseVO<Integer> resp = employeeFeignClient.save(req);
            if (resp != null && Objects.equals(resp.getStatus(), 200)) {
                outboxService.markSuccess(outbox.getId());
                log.info("保存员工成功id：{}", resp.getData());
            } else {
                String error = "employee 返回非 200: " + (resp == null ? "null" : resp.getStatus());
                log.error("保存用户失败, {}", error);
                outboxService.markRetry(outbox, error);
            }
        } catch (Exception e) {
            log.error("调用 employee 服务失败，已记录待补偿", e);
            outboxService.markRetry(outbox, e.getMessage());
        }
    }

    @Override
    public void afterPropertiesSet() {
        eventBus.register(this);
    }
}
