package com.yjx.b2c.shop.feign;

import com.yjx.b2c.common.response.BaseResponseVO;
import com.yjx.b2c.shop.feign.request.AddEmployeeRequestVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author yjx
 * @date 2026/9/11
 * @description
 */

@FeignClient(name = "employee-service", path = "/employee")
public interface EmployeeFeignClient {

    @PostMapping("/save")
    public BaseResponseVO<Integer> save(@RequestBody AddEmployeeRequestVO req);
}

