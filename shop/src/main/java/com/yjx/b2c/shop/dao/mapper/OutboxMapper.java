package com.yjx.b2c.shop.dao.mapper;

import com.yjx.b2c.shop.dao.po.OutboxPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OutboxMapper {

    int insert(OutboxPO po);

    OutboxPO findById(@Param("id") Integer id);

    List<OutboxPO> findRetryable(@Param("maxRetry") int maxRetry, @Param("limit") int limit);

    int markSuccess(@Param("id") Integer id);

    int markRetry(@Param("id") Integer id, @Param("lastError") String lastError);
}
