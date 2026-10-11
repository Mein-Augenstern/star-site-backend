package com.xp.starsite.common.log;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;

/**
 * Feign 出站头透传：每个请求带上 traceId 与调用方应用名，
 * 服务端过滤器据此还原链路并留痕「谁调的我」。
 *
 * @author xiaopu
 * @since 2026-10
 */
public class FeignTraceInterceptor implements RequestInterceptor {

    private final String callerApp;

    public FeignTraceInterceptor(@Value("${spring.application.name:unknown}") String callerApp) {
        this.callerApp = callerApp;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header(TraceContext.HEADER_TRACE_ID, TraceContext.current());
        template.header(TraceContext.HEADER_CALLER_APP, callerApp);
    }
}
