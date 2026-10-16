package com.yjx.b2c.shop.service.event.handler;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.common.response.BaseResponseVO;
import com.yjx.b2c.shop.dao.po.OutboxPO;
import com.yjx.b2c.shop.feign.EmployeeFeignClient;
import com.yjx.b2c.common.contract.AddEmployeeRequestVO;
import com.yjx.b2c.shop.service.OutboxService;
import com.yjx.b2c.shop.service.event.ShopRegisterEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 注册事件订阅者行为测试（任务项 4.2.x / 6.3.x）。使用同步 EventBus，避免依赖线程等待。
 */
class InitEmployeeEventHandleTest {

    private EventBus eventBus;
    private EmployeeFeignClient employeeFeignClient;
    private OutboxService outboxService;
    private InitEmployeeEventHandle handle;

    @BeforeEach
    void setUp() throws Exception {
        eventBus = new EventBus();
        employeeFeignClient = mock(EmployeeFeignClient.class);
        outboxService = mock(OutboxService.class);

        OutboxPO pending = new OutboxPO();
        pending.setId(1);
        when(outboxService.enqueue(anyString(), any(), any())).thenReturn(pending);

        handle = new InitEmployeeEventHandle();
        ReflectionTestUtils.setField(handle, "eventBus", eventBus);
        ReflectionTestUtils.setField(handle, "employeeFeignClient", employeeFeignClient);
        ReflectionTestUtils.setField(handle, "outboxService", outboxService);
        handle.afterPropertiesSet();
    }

    private ShopRegisterEvent event() {
        ShopRegisterEvent event = new ShopRegisterEvent();
        event.setShopId(88);
        event.setShopName("测试店铺");
        event.setAdminAccount("admin");
        event.setAdminPwd("123456");
        return event;
    }

    @Test
    @DisplayName("4.2.1/4.2.2/4.2.3 收到注册事件后按契约调用 employee 服务")
    void callsEmployeeService() {
        when(employeeFeignClient.save(any(AddEmployeeRequestVO.class))).thenReturn(new BaseResponseVO<>(7));

        eventBus.post(event());

        ArgumentCaptor<AddEmployeeRequestVO> captor = ArgumentCaptor.forClass(AddEmployeeRequestVO.class);
        verify(employeeFeignClient).save(captor.capture());
        AddEmployeeRequestVO req = captor.getValue();
        assertThat(req.getShopId()).isEqualTo(88);
        assertThat(req.getUsername()).isEqualTo("admin");
        assertThat(req.getPassword()).isEqualTo("123456");
    }

    @Test
    @DisplayName("6.3.1 [红灯] 调用成功时把补偿记录标记为成功")
    void successMarksOutbox() {
        when(employeeFeignClient.save(any(AddEmployeeRequestVO.class))).thenReturn(new BaseResponseVO<>(7));

        eventBus.post(event());

        verify(outboxService).markSuccess(1);
    }

    @Test
    @DisplayName("6.3.1 [红灯] employee 返回非 200 时标记待重试")
    void nonSuccessMarksRetry() {
        when(employeeFeignClient.save(any(AddEmployeeRequestVO.class)))
                .thenReturn(new BaseResponseVO<>(500, "保存用户失败"));

        assertThatCode(() -> eventBus.post(event())).doesNotThrowAnyException();

        verify(outboxService).markRetry(any(OutboxPO.class), contains("500"));
    }

    @Test
    @DisplayName("6.3.1 [红灯] employee 调用异常时留下待补偿记录且不外抛")
    void feignFailureRecordsRetry() {
        when(employeeFeignClient.save(any(AddEmployeeRequestVO.class)))
                .thenThrow(new RuntimeException("connect timed out"));

        assertThatCode(() -> eventBus.post(event())).doesNotThrowAnyException();

        verify(outboxService).markRetry(any(OutboxPO.class), contains("timed out"));
    }

    @Test
    @DisplayName("4.2.4 employee 返回非 200 时不抛异常（不影响主流程）")
    void nonSuccessResponseDoesNotThrow() {
        when(employeeFeignClient.save(any(AddEmployeeRequestVO.class)))
                .thenReturn(new BaseResponseVO<>(500, "保存用户失败"));

        assertThatCode(() -> eventBus.post(event())).doesNotThrowAnyException();
    }
}
