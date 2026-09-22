package com.yjx.b2c.shop.dao.mapper;

import com.yjx.b2c.shop.dao.po.MessagePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author yjx
 * @date 2026/9/15
 * @description
 */


@Mapper
public interface MessageMapper {

    Integer saveMessage(MessagePO messagePO);

}
