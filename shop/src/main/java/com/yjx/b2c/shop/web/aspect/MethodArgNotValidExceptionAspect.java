package com.yjx.b2c.shop.web.aspect;

import com.yjx.b2c.common.response.BaseResponseVO;
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
 * @date 2026/9/15
 * @description
 */
@RestControllerAdvice
public class MethodArgNotValidExceptionAspect {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public BaseResponseVO<?> handleException(MethodArgumentNotValidException ex) {
        List<ObjectError> errors = ex.getAllErrors();
        if (CollectionUtils.isEmpty(errors)) {
            return new BaseResponseVO<>(500, "内部错误");
        }
        String errorMsg = errors.stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(","));
        return new BaseResponseVO<>(400, errorMsg);
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    public BaseResponseVO<?> handleIllegalArgument(IllegalArgumentException ex) {
        return new BaseResponseVO<>(400, ex.getMessage());
    }
}