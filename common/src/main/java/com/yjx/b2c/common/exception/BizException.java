package com.yjx.b2c.common.exception;

import lombok.Getter;

/**
 * 业务异常：携带业务错误码，由各服务的异常处理器统一转换为 BaseResponseVO。
 * 与参数校验失败（IllegalArgumentException）区分：前者是业务规则，后者是请求格式。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException badRequest(String message) {
        return new BizException(400, message);
    }

    public static BizException serverError(String message) {
        return new BizException(500, message);
    }
}
