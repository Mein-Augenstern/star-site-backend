package com.xp.starsite.common.error;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 全平台统一异常错误对象（CommonError）：所有服务失败响应 {@code R.error} 的唯一结构。
 * <p>
 * 三条约定：
 * <ul>
 *   <li>结构统一——任何服务的任何失败，调用方拿到的 error 字段形状一致
 *       （code/type/message/detail/stackTrace/traceId/timestamp），禁止各服务自造错误结构；</li>
 *   <li>堆栈留存——系统异常（5xx 语义）必须捕获完整堆栈进 stackTrace；
 *       业务拒绝（BizException 等预期失败）无堆栈，stackTrace 为 null；</li>
 *   <li>traceId 必填——与日志体系（DigestLoggers/TraceContext）同源，报错后可凭
 *       traceId 直达对应日志行。</li>
 * </ul>
 * 暴露面提示：stackTrace 会随 HTTP/RPC 响应出服务（家庭内网平台，诊断价值优先），
 * 对外公网网关若开放，应在边缘网关剥离该字段。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class CommonError implements Serializable {

    private int code;

    /** 异常类名（如 com.xp.starsite.common.exception.BizException），预期失败与系统异常可据此区分 */
    private String type;

    /** 用户可读文案（与 R.message 一致） */
    private String message;

    /** 技术细节（异常自身 message，含定位线索；预期失败时通常与 message 相同） */
    private String detail;

    /** 异常堆栈（系统异常捕获；预期失败为 null） */
    private String stackTrace;

    /** 链路 id（与日志同源，报错后检索日志的键） */
    private String traceId;

    /** 发生时间（ISO-8601） */
    private LocalDateTime timestamp;

    /** 预期失败（业务拒绝）：无堆栈 */
    public static CommonError of(int code, String type, String message, String traceId) {
        CommonError error = new CommonError();
        error.setCode(code);
        error.setType(type);
        error.setMessage(message);
        error.setDetail(message);
        error.setTraceId(traceId);
        error.setTimestamp(LocalDateTime.now());
        return error;
    }

    /** 系统异常：捕获完整堆栈 */
    public static CommonError ofThrowable(int code, Throwable ex, String traceId) {
        CommonError error = new CommonError();
        error.setCode(code);
        error.setType(ex.getClass().getName());
        error.setMessage(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        error.setDetail(ex.toString());
        error.setStackTrace(stackTraceOf(ex));
        error.setTraceId(traceId);
        error.setTimestamp(LocalDateTime.now());
        return error;
    }

    private static String stackTraceOf(Throwable ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        // 堆栈截断在 8KB：R 报文别被超大堆栈撑爆，全量在日志文件里
        String stack = sw.toString();
        return stack.length() > 8192 ? stack.substring(0, 8192) + "\n...(truncated, full in log)" : stack;
    }
}
