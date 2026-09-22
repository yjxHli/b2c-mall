package com.yjx.b2c.product.dao.mapper;

import com.yjx.b2c.product.dao.po.CategoryPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author yjx
 * @date 2026/9/18
 * @description
 */
@Mapper
public interface CategoryMapper {

    CategoryPO findById(@Param("id") Integer id);

    int countChildren(@Param("parentId") Integer parentId);
}
