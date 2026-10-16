package com.yjx.b2c.shop.service.impl;

import com.yjx.b2c.shop.dao.mapper.MessageMapper;
import com.yjx.b2c.shop.dao.po.MessagePO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 站内信服务测试（任务项 5.2.x）。
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageMapper messageMapper;
    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    @DisplayName("5.2.2 写入欢迎消息时 msgType=1、isRead=0")
    void savesWelcomeMessageWithDefaults() {
        when(messageMapper.saveMessage(any(MessagePO.class))).thenReturn(1);

        messageService.saveWeclomeMessage(88, "欢迎注册本系统", "系统规范，系统规约");

        ArgumentCaptor<MessagePO> captor = ArgumentCaptor.forClass(MessagePO.class);
        verify(messageMapper).saveMessage(captor.capture());
        assertThat(captor.getValue().getMsgType()).isEqualTo(1);
        assertThat(captor.getValue().getIsRead()).isZero();
    }

    @Test
    @DisplayName("5.2.1 店铺 ID 为空时拒绝写入")
    void rejectsMissingShopId() {
        assertThatThrownBy(() -> messageService.saveWeclomeMessage(null, "标题", "内容"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("5.2.2 影响行数不为 1 时抛出业务异常")
    void failsWhenRowCountIsZero() {
        when(messageMapper.saveMessage(any(MessagePO.class))).thenReturn(0);

        assertThatThrownBy(() -> messageService.saveWeclomeMessage(88, "标题", "内容"))
                .isInstanceOf(com.yjx.b2c.common.exception.BizException.class)
                .hasMessage("保存欢迎消息异常");
    }
}
