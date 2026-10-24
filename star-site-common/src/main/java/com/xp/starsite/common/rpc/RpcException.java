package com.xp.starsite.common.rpc;

import lombok.Getter;

/**
 * 标准 RPC 调用异常：跨服务调用失败（网络/超时/业务码非 0）的统一出口。
 * 由 {@link RpcCallGuard} 翻译产生，integration 层与消费方只认它一种 RPC 失败语义；
 * retryable 标注失败是否可重试（超时/依赖未就绪/5xx = true，参数与权限类 4xx = false），
 * 来源优先取响应体 R.retryable（提供方声明），缺省按状态段位推断。
 *
 * @author muyi
 * @since 2026-10
 */
@Getter
public class RpcException extends RuntimeException {

    private final int code;

    private final boolean retryable;

    public RpcException(int code, String message, boolean retryable) {
        super(message);
        this.code = code;
        this.retryable = retryable;
    }

    public RpcException(int code, String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.retryable = retryable;
    }
}
