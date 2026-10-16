package com.yjx.b2c.employee.web.aspect;

import com.yjx.b2c.common.exception.BizException;
import com.yjx.b2c.common.response.BaseResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 统一把业务校验异常转成 BaseResponseVO
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponseVO<?> handleIllegalArgument(IllegalArgumentException ex) {
        return new BaseResponseVO<>(400, ex.getMessage());
    }

    @ExceptionHandler(BizException.class)
    public BaseResponseVO<?> handleBizException(BizException ex) {
        return new BaseResponseVO<>(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponseVO<?> handleValidation(MethodArgumentNotValidException ex) {
        List<ObjectError> errors = ex.getAllErrors();
        if (CollectionUtils.isEmpty(errors)) {
            return new BaseResponseVO<>(500, "内部错误");
        }
        String message = errors.stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(","));
        return new BaseResponseVO<>(400, message);
    }

    /**
     * 未预期异常的兜底：只接 RuntimeException，避免截胡 Spring MVC 自身的 405/415 基础设施异常。
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponseVO<?> handleUnexpected(RuntimeException ex) {
        log.error("接口未预期异常", ex);
        return new BaseResponseVO<>(500, "服务器内部错误");
    }
}
