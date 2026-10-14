package com.xp.starsite.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xp.starsite.common.error.CommonError;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体：{code, message, data[, retryable]}
 *
 * <p>全平台所有 REST 接口的出参标准封装：controller 返回 {@code R.ok(data)} / {@code R.fail(...)}，
 * 前端与跨服务调用方按 code 判定成败，配合 ResultCode 统一错误语义；
 * 失败响应可携带 retryable 供 RPC 消费方判定重试策略（见字段注释）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class R<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    /**
     * 失败响应可选携带的可重试标记：true=调用方可重试（依赖超时/未就绪/5xx），
     * false=业务性失败重试无意义；null=未声明（成功响应恒为 null）。
     * 跨服务消费方经 RpcCallGuard 消费该语义，HTTP 前端可忽略本字段。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean retryable;

    /**
     * 统一异常错误对象（失败响应携带，成功恒 null）：code/type/message/detail/
     * stackTrace/traceId/timestamp，全平台唯一异常结构（见 CommonError 类注释）。
     * 由全局异常处理器与业务模板填充；前端可忽略，RPC 消费方与排障工具消费。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private CommonError error;

    /** 失败响应挂统一错误对象（链式） */
    public R<T> withError(CommonError error) {
        this.error = error;
        return this;
    }

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

    /** 失败响应（带可重试标记，供 RPC 消费方判定重试策略） */
    public static <T> R<T> fail(int code, String message, boolean retryable) {
        R<T> r = build(code, message, null);
        r.setRetryable(retryable);
        return r;
    }

    public static <T> R<T> build(int code, String message, T data) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        r.setData(data);
        return r;
    }
}
