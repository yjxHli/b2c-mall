package com.yjx.b2c.order.dao.mapper;

import com.yjx.b2c.order.dao.po.OrderPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author yjx
 * @date 2026/9/18
 * @description
 */
@Mapper
public interface OrderMapper {

    OrderPO findById(@Param("id") Integer id);

    Integer save(OrderPO po);

    /** 支付成功：仅当订单仍为待支付(0)时才更新为已支付(1) */
    int markPaid(@Param("id") Integer id, @Param("payType") Integer payType, @Param("tradeNo") String tradeNo);

    /** 发货：仅当订单仍为已支付(1)时才更新为已发货(2) */
    int ship(@Param("id") Integer id, @Param("logisticsNo") String logisticsNo, @Param("operatorId") Integer operatorId);
}
