package com.yjx.b2c.product.dao.mapper;

import com.yjx.b2c.product.dao.po.SkuPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author yjx
 * @date 2026/9/18
 * @description
 */
@Mapper
public interface SkuMapper {

    Integer save(SkuPO po);

    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);
}
