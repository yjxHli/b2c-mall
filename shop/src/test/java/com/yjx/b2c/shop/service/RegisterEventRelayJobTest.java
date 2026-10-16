package com.yjx.b2c.shop.service;

import com.yjx.b2c.common.response.BaseResponseVO;
import com.yjx.b2c.shop.dao.po.OutboxPO;
import com.yjx.b2c.shop.feign.EmployeeFeignClient;
import com.yjx.b2c.common.contract.AddEmployeeRequestVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 补偿重试任务测试（任务项 6.3.2 / 6.3.3）。
 */
class RegisterEventRelayJobTest {

    private OutboxService outboxService;
    private EmployeeFeignClient employeeFeignClient;
    private RegisterEventRelayJob job;

    @BeforeEach
    void setUp() {
        outboxService = mock(OutboxService.class);
        employeeFeignClient = mock(EmployeeFeignClient.class);
        job = new RegisterEventRelayJob(outboxService, employeeFeignClient);
    }

    private OutboxPO pending() {
        OutboxPO po = new OutboxPO();
        po.setId(9);
        po.setEventType("EMPLOYEE_INIT");
        po.setAggregateId(88);
        po.setPayload("{\"shopId\":88,\"username\":\"admin\",\"password\":\"123456\"}");
        po.setStatus(OutboxPO.STATUS_RETRY);
        po.setRetryCount(1);
        return po;
    }

    @Test
    @DisplayName("6.3.2 重试成功时还原负载、调用 employee 并标记成功")
    void retrySuccessMarksSuccess() {
        when(outboxService.findRetryable(anyInt(), anyInt())).thenReturn(List.of(pending()));
        when(employeeFeignClient.save(any(AddEmployeeRequestVO.class))).thenReturn(new BaseResponseVO<>(7));

        job.relay();

        ArgumentCaptor<AddEmployeeRequestVO> captor = ArgumentCaptor.forClass(AddEmployeeRequestVO.class);
        verify(employeeFeignClient).save(captor.capture());
        assertThat(captor.getValue().getShopId()).isEqualTo(88);
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
        verify(outboxService).markSuccess(9);
    }

    @Test
    @DisplayName("6.3.3 重试仍然失败时继续标记待重试")
    void retryFailureMarksRetryAgain() {
        when(outboxService.findRetryable(anyInt(), anyInt())).thenReturn(List.of(pending()));
        when(employeeFeignClient.save(any(AddEmployeeRequestVO.class)))
                .thenThrow(new RuntimeException("still down"));

        job.relay();

        verify(outboxService).markRetry(any(OutboxPO.class), contains("still down"));
    }

    @Test
    @DisplayName("6.3.3 没有待补偿记录时不做任何调用")
    void nothingToRetry() {
        when(outboxService.findRetryable(anyInt(), anyInt())).thenReturn(Collections.emptyList());

        job.relay();

        verifyNoInteractions(employeeFeignClient);
    }
}
