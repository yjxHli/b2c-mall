package com.yjx.b2c.shop.dao.mapper;

import com.yjx.b2c.shop.dao.po.OutboxPO;
import com.yjx.b2c.shop.support.ShopDaoTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 注册事件本地消息表持久化测试（任务项 6.3.x）。
 */
class OutboxMapperTest extends ShopDaoTestSupport {

    private OutboxPO po(String eventType) {
        OutboxPO po = new OutboxPO();
        po.setEventType(eventType);
        po.setAggregateId(88);
        po.setPayload("{\"shopId\":88}");
        po.setStatus(OutboxPO.STATUS_PENDING);
        return po;
    }

    @Test
    @DisplayName("6.3.1 入队后立即成为可重试记录并回填主键")
    void insertAndFindRetryable() {
        OutboxPO po = po("EMPLOYEE_INIT");

        assertThat(outboxMapper.insert(po)).isEqualTo(1);
        assertThat(po.getId()).isNotNull();
        assertThat(outboxMapper.findRetryable(5, 10)).extracting(OutboxPO::getId).containsExactly(po.getId());
    }

    @Test
    @DisplayName("6.3.3 markSuccess 之后不再参与重试")
    void markSuccessExcludesFromRetry() {
        OutboxPO po = po("EMPLOYEE_INIT");
        outboxMapper.insert(po);

        outboxMapper.markSuccess(po.getId());

        assertThat(outboxMapper.findRetryable(5, 10)).isEmpty();
        assertThat(outboxMapper.findById(po.getId()).getStatus()).isEqualTo(OutboxPO.STATUS_SUCCESS);
    }

    @Test
    @DisplayName("6.3.3 markRetry 累加重试次数、记录错误并进入退避")
    void markRetryIncrementsAndBacksOff() {
        OutboxPO po = po("EMPLOYEE_INIT");
        outboxMapper.insert(po);

        outboxMapper.markRetry(po.getId(), "connect timed out");

        OutboxPO after = outboxMapper.findById(po.getId());
        assertThat(after.getStatus()).isEqualTo(OutboxPO.STATUS_RETRY);
        assertThat(after.getRetryCount()).isEqualTo(1);
        assertThat(after.getLastError()).isEqualTo("connect timed out");
        assertThat(outboxMapper.findRetryable(5, 10)).isEmpty();
    }

    @Test
    @DisplayName("6.3.3 达到最大重试次数的记录不再被拾取")
    void exceedsMaxRetryIsNotPicked() {
        OutboxPO po = po("EMPLOYEE_INIT");
        outboxMapper.insert(po);

        assertThat(outboxMapper.findRetryable(0, 10)).isEmpty();
    }
}
