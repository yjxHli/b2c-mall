package com.yjx.b2c.employee.dao.mapper;

import com.yjx.b2c.employee.dao.po.EmployeePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author yjx
 * @date 2026/9/15
 * @description
 */
@Mapper
public interface EmployeeMapper {
    Integer save(EmployeePO po);

    EmployeePO findByUsername(@Param("username") String username, @Param("shopId") Integer shopId);

    int updateLoginStatus(@Param("id") Integer id);
}
