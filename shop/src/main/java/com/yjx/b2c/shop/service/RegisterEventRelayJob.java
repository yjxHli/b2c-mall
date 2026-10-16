package com.yjx.b2c.shop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yjx.b2c.common.response.BaseResponseVO;
import com.yjx.b2c.shop.dao.po.OutboxPO;
import com.yjx.b2c.shop.feign.EmployeeFeignClient;
import com.yjx.b2c.common.contract.AddEmployeeRequestVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 补偿重试任务：扫描本地消息表，重放调用 employee 服务。
 * 重放必须幂等，由 employee 侧的 (shop_id, username) 唯一约束与查重逻辑保证。
 */
@Component
@Slf4j
public class RegisterEventRelayJob {

    private static final int MAX_RETRY = 5;
    private static final int BATCH_SIZE = 50;

    private final OutboxService outboxService;
    private final EmployeeFeignClient employeeFeignClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RegisterEventRelayJob(OutboxService outboxService, EmployeeFeignClient employeeFeignClient) {
        this.outboxService = outboxService;
        this.employeeFeignClient = employeeFeignClient;
    }

    @Scheduled(fixedDelay = 30_000)
    public void relay() {
        List<OutboxPO> retryable = outboxService.findRetryable(MAX_RETRY, BATCH_SIZE);
        for (OutboxPO po : retryable) {
            try {
                AddEmployeeRequestVO req = objectMapper.readValue(po.getPayload(), AddEmployeeRequestVO.class);
                BaseResponseVO<Integer> resp = employeeFeignClient.save(req);
                if (resp != null && Objects.equals(resp.getStatus(), 200)) {
                    outboxService.markSuccess(po.getId());
                    log.info("补偿重试成功, outboxId={}", po.getId());
                } else {
                    outboxService.markRetry(po,
                            "retry failed, status=" + (resp == null ? "null" : resp.getStatus()));
                }
            } catch (Exception e) {
                log.warn("补偿重试失败, outboxId={}", po.getId(), e);
                outboxService.markRetry(po, e.getMessage());
            }
        }
    }
}
