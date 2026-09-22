package com.yjx.b2c.product.dao.mapper;

import com.yjx.b2c.product.dao.po.SkuLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author yjx
 * @date 2026/9/18
 * @description
 */
@Mapper
public interface SkuLogMapper {

    Integer save(SkuLogPO po);
}
