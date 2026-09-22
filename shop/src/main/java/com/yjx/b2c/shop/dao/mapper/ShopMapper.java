package com.yjx.b2c.shop.dao.mapper;

import com.yjx.b2c.shop.dao.po.ShopPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author yjx
 * @date 2026/9/15
 * @description
 */

@Mapper
public interface ShopMapper {

    Integer registerShop(ShopPO shopPO);

    int exist(@Param("shopName") String shopName);

    ShopPO findByAccount(@Param("adminAccount") String adminAccount);

    int updateLoginStatus(@Param("id") Integer id);
}
