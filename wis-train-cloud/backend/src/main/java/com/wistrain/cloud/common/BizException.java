package com.wistrain.cloud.common;

import lombok.Getter;

/**
 * 业务异常：由 GlobalExceptionHandler 统一转为 ApiResult
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String message) {
        this(1, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
