package com.yjx.b2c.shop.dao.mapper;

import com.yjx.b2c.shop.dao.po.LoginLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author yjx
 * @date 2026/9/18
 * @description
 */
@Mapper
public interface LoginLogMapper {
    Integer saveLoginLog(LoginLogPO po);
}
