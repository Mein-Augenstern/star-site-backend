package com.xp.starsite.common.log;

import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.Response;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Feign 出站留痕客户端（rpc-client-digest）：本服务作为客户端调别人的统一出口。
 * 包装原生 {@link Client}，记录目标服务/方法/URL/状态/耗时；traceId 与调用方应用名经头透传。
 * 由 {@link StarSiteLogConfig} 注册为 @Primary，所有 Feign 客户端自动走此包装。
 *
 * @author xiaopu
 * @since 2026-10
 */
public class FeignDigestClient implements Client {

    /** 服务名提取用：URL 形如 http://star-family/internal/... */
    private static final String SERVICE_URL_MARK = "://";

    private final Client delegate;
    private final String callerApp;

    public FeignDigestClient(Client delegate, String callerApp) {
        this.delegate = delegate;
        this.callerApp = callerApp;
    }

    @Override
    public Response execute(Request request, Options options) throws IOException {
        long start = System.currentTimeMillis();
        String target = targetService(request.url());
        String traceId = TraceContext.current();
        try {
            Response response = delegate.execute(request, options);
            DigestLoggers.emit(DigestLoggers.RPC_CLIENT, DigestLoggers.fields(
                    "span", "client",
                    "method", request.httpMethod().name(),
                    "target", target,
                    "url", stripQuery(request.url()),
                    "status", response.status(),
                    "costMs", System.currentTimeMillis() - start,
                    "caller.app", callerApp));
            return response;
        } catch (IOException e) {
            DigestLoggers.emit(DigestLoggers.RPC_CLIENT, DigestLoggers.fields(
                    "span", "client",
                    "method", request.httpMethod().name(),
                    "target", target,
                    "url", stripQuery(request.url()),
                    "status", "EXCEPTION",
                    "error", e.getMessage(),
                    "costMs", System.currentTimeMillis() - start,
                    "caller.app", callerApp));
            throw e;
        }
    }

    /** 从 URL 提取目标服务名（nacos 服务名即 host） */
    private String targetService(String url) {
        int after = url.indexOf(SERVICE_URL_MARK);
        if (after < 0) {
            return "unknown";
        }
        String rest = url.substring(after + SERVICE_URL_MARK.length());
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }

    private String stripQuery(String url) {
        int q = url.indexOf('?');
        return q < 0 ? url : url.substring(0, q);
    }

    /** 透传头构造（供 RequestInterceptor 使用）：traceId + 调用方应用名 */
    public static void propagateHeaders(Map<String, Collection<String>> headerTemplate,
                                         String traceId, String callerApp) {
        headerTemplate.put(TraceContext.HEADER_TRACE_ID, java.util.Collections.singletonList(traceId));
        headerTemplate.put(TraceContext.HEADER_CALLER_APP, java.util.Collections.singletonList(callerApp));
    }
}
