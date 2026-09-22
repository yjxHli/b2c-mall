package com.yjx.b2c.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yjx
 * @date 2026/9/15
 * @description
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponseVO<T> {
    /***
     * 状态吗，200成功0，400请求错误，500服务器内部错误
     */
    private int status = 200;
    private String message;
    private T data;
    public BaseResponseVO(int status, String message) {
        this.status = status;
        this.message = message;
    }
    public BaseResponseVO(T data) {
        this.data = data;
    }
}
