package com.wistrain.cloud.common;

import lombok.Data;

/**
 * 统一响应格式（NF-14）：{ code, message, data }，code=0 成功
 */
@Data
public class ApiResult<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 0;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static ApiResult<Void> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> fail(String message) {
        return fail(1, message);
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        ApiResult<T> r = new ApiResult<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
