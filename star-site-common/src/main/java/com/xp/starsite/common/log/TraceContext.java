package com.xp.starsite.common.log;

import org.slf4j.MDC;

/**
 * 链路追踪上下文（SOFA Tracer 的轻量本土化）：traceId 走 MDC，跨服务经 HTTP 头/MQ 消息属性传播。
 * <p>
 * 生命周期：入口（{@code RpcServerDigestFilter} 或 MQ 消费端）set → 贯穿本服务全部日志（logback %X{traceId}）
 * 与 digest 日志字段 → 出口（Feign 调用/MQ 发送）透传给下游 → 请求结束 clear（防线程池串号）。
 * 无 traceId 时由入口生成（32 位 hex，兼容 SOFA Tracer 风格）。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class TraceContext {

    /** HTTP 传播头（网关→服务、服务→服务） */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    /** HTTP 传播头：调用方应用名（服务端 digest 留痕「谁调的我」） */
    public static final String HEADER_CALLER_APP = "X-Caller-App";
    /** MQ 消息属性键（发送方→消费方传播） */
    public static final String MQ_KEY_TRACE_ID = "trace_id";

    private static final String MDC_TRACE_ID = "traceId";

    private TraceContext() {
    }

    /** 取当前 traceId；无则生成并写入 MDC（非入口场景兜底，如定时任务线程） */
    public static String current() {
        String tid = MDC.get(MDC_TRACE_ID);
        if (tid == null || tid.isBlank()) {
            tid = generate();
            MDC.put(MDC_TRACE_ID, tid);
        }
        return tid;
    }

    /** 入口设置（已携带上游 traceId 或新生成） */
    public static void set(String traceId) {
        MDC.put(MDC_TRACE_ID, traceId == null || traceId.isBlank() ? generate() : traceId);
    }

    /** 请求结束必须调用：清理 MDC，防止线程池复用串号 */
    public static void clear() {
        MDC.remove(MDC_TRACE_ID);
    }

    /** 32 位 hex traceId，风格对齐 SOFA Tracer */
    public static String generate() {
        return java.util.UUID.randomUUID().toString().replace("-", "")
                + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                + Long.toHexString(System.currentTimeMillis());
    }
}
