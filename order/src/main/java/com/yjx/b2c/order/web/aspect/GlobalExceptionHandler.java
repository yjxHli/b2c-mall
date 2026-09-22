package com.yjx.b2c.order.web.aspect;

import com.yjx.b2c.common.response.BaseResponseVO;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 统一把业务校验异常转成 BaseResponseVO
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponseVO<?> handleIllegalArgument(IllegalArgumentException ex) {
        return new BaseResponseVO<>(400, ex.getMessage());
    }
}
