package com.xp.starsite.common.exception;

import com.xp.starsite.common.error.CommonError;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.common.log.TraceContext;
import com.xp.starsite.common.rpc.RpcException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：所有异常统一转成 R 响应体（失败挂全平台统一 {@link CommonError}）。
 *
 * <p>全平台各 Web 服务以组件扫描引入本 @RestControllerAdvice：
 * 业务异常 BizException 按 code 透出（预期失败，无堆栈）、RPC 异常透传 retryable、
 * 参数校验失败转 400、未知异常兜底 500 且捕获完整堆栈进 error.stackTrace（不泄露给
 * message 字段，全量在日志文件）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常（预期失败：error 带类型与 traceId，无堆栈） */
    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        R<Void> r = R.fail(e.getCode(), e.getMessage(), e.getCode() >= 500);
        r.withError(CommonError.of(e.getCode(), e.getClass().getName(), e.getMessage(), TraceContext.current()));
        return r;
    }

    /** RPC 调用异常（内部接口串联下游时冒泡，retryable 随异常透出） */
    @ExceptionHandler(RpcException.class)
    public R<Void> handleRpcException(RpcException e) {
        log.warn("RPC 异常: code={}, message={}, retryable={}", e.getCode(), e.getMessage(), e.isRetryable());
        R<Void> r = R.fail(e.getCode(), e.getMessage(), e.isRetryable());
        r.withError(CommonError.of(e.getCode(), e.getClass().getName(), e.getMessage(), TraceContext.current()));
        return r;
    }

    /** 参数校验失败（@Valid） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "请求参数错误";
        R<Void> r = R.fail(ResultCode.BAD_REQUEST.getCode(), message, false);
        r.withError(CommonError.of(ResultCode.BAD_REQUEST.getCode(),
                e.getClass().getName(), message, TraceContext.current()));
        return r;
    }

    /** 兜底（系统异常：error 捕获完整堆栈，message 字段仍不泄露） */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        R<Void> r = R.fail(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMessage(), true);
        r.withError(CommonError.ofThrowable(ResultCode.SYSTEM_ERROR.getCode(), e, TraceContext.current()));
        return r;
    }
}
