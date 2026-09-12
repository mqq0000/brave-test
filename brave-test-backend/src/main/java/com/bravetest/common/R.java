package com.bravetest.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一响应体
 */
@Data
@Schema(description = "统一响应体")
public class R<T> {

    @Schema(description = "业务码，0=成功")
    private int code;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "数据")
    private T data;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 0;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(String message) {
        return fail(400, message);
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
