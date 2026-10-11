package com.xp.starsite.common.log;

import feign.Request;
import feign.Response;
import feign.Util;

import java.io.IOException;

/**
 * Feign 出站留痕（rpc-client-digest）：本服务作为客户端调内部服务的统一出口。
 * <p>
 * 选择 feign.Logger 扩展点而非包装 feign.Client Bean：Logger 与 Client 类型无交集，
 * 不会造成「@Primary Client 注入自身」的循环依赖；且 feign 原生回调自带耗时（elapsedTime）。
 * <p>
 * 生效条件：客户端 logger-level ≥ BASIC（compose app-env 已配 BASIC），
 * BASIC 级别下本类负责响应体重缓冲（feign 契约：logAndRebufferResponse 后响应可重读）。
 * 只记 digest 不落 feign 原生日志行；URL 只保留到路径（去 query 防敏感参数入盘）。
 *
 * @author xiaopu
 * @since 2026-10
 */
public class FeignDigestLogger extends feign.Logger {

    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
        // 请求侧不记：响应回调统一记录（含状态/耗时，一次一行）
    }

    @Override
    protected Response logAndRebufferResponse(String configKey, Level logLevel,
                                              Response response, long elapsedTime) throws IOException {
        // BASIC 级别下必须重缓冲响应体，保证调用方仍能读取响应；elapsedTime 单位为微秒
        long costMs = elapsedTime / 1000;
        if (response.body() != null) {
            byte[] bodyData = Util.toByteArray(response.body().asInputStream());
            emit(response, costMs);
            return response.toBuilder().body(bodyData).build();
        }
        emit(response, costMs);
        return response;
    }

    @Override
    protected IOException logIOException(String configKey, Level logLevel, IOException ioe, long elapsedTime) {
        DigestLoggers.emit(DigestLoggers.RPC_CLIENT, DigestLoggers.fields(
                "span", "client", "configKey", configKey,
                "status", "EXCEPTION", "error", ioe.getMessage(),
                "costMs", elapsedTime / 1000));
        return ioe;
    }

    private void emit(Response response, long costMs) {
        feign.Request request = response.request();
        DigestLoggers.emit(DigestLoggers.RPC_CLIENT, DigestLoggers.fields(
                "span", "client",
                "method", request.httpMethod().name(),
                "target", targetService(request.url()),
                "url", stripQuery(request.url()),
                "status", response.status(),
                "costMs", costMs));
    }

    /** 从 URL 提取目标服务名（nacos 服务名即 host） */
    private String targetService(String url) {
        int after = url.indexOf("://");
        if (after < 0) {
            return "unknown";
        }
        String rest = url.substring(after + 3);
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }

    private String stripQuery(String url) {
        int q = url.indexOf('?');
        return q < 0 ? url : url.substring(0, q);
    }

    /** feign 原生日志行静默（digest 之外不重复输出） */
    @Override
    protected void log(String configKey, String format, Object... args) {
    }
}
