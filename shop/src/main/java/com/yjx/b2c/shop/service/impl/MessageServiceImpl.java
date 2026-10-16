package com.yjx.b2c.shop.service.impl;

import cn.hutool.core.lang.Assert;
import com.yjx.b2c.common.exception.BizException;
import com.yjx.b2c.shop.dao.mapper.MessageMapper;
import com.yjx.b2c.shop.dao.po.MessagePO;
import com.yjx.b2c.shop.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yjx
 * @date 2026/9/15
 * @description
 */
@Service
public class MessageServiceImpl implements MessageService {
    @Autowired
    private MessageMapper messageMapper;

    @Override
    public void saveWeclomeMessage(Integer shopId, String title, String content) {
        Assert.notNull(shopId);
        Assert.notBlank(content);
        Assert.notBlank(title);

        MessagePO po = new MessagePO();
        po.setShopId(shopId);
        po.setTitle(title);
        po.setContent(content);
        po.setMsgType(1);
        po.setIsRead(0);
        Integer row = messageMapper.saveMessage(po);
        if (row == null || row != 1) {
            throw BizException.serverError("保存欢迎消息异常");
        }
    }
}
