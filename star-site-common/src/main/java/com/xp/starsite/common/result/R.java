package com.xp.starsite.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体：{code, message, data}
 *
 * <p>全平台所有 REST 接口的出参标准封装：controller 返回 {@code R.ok(data)} / {@code R.fail(...)}，
 * 前端与跨服务调用方按 code 判定成败，配合 ResultCode 统一错误语义。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class R<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok() {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> ok(T data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return build(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> R<T> fail(int code, String message) {
        return build(code, message, null);
    }

    public static <T> R<T> build(int code, String message, T data) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        r.setData(data);
        return r;
    }
}
