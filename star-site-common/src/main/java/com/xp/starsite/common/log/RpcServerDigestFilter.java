package com.xp.starsite.common.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * 服务端请求留痕过滤器（rpc-server-digest）：本服务作为服务端被调用的统一入口。
 * <p>
 * 职责三合一：① 提取/生成 traceId 并入 MDC；② 请求结束输出 rpc-server-digest
 * （method/path/status/cost/caller——caller 取 X-Caller-App 头，谁调的我一目了然）；
 * ③ 请求结束清理 MDC。中间件层自动生效，业务代码零侵入。
 *
 * @author muyi
 * @since 2026-10
 */
public class RpcServerDigestFilter extends OncePerRequestFilter {

    /** 探针端点不打 digest（健康检查噪音） */
    private static final String ACTUATOR_PREFIX = "/actuator";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        TraceContext.set(request.getHeader(TraceContext.HEADER_TRACE_ID));
        String caller = request.getHeader(TraceContext.HEADER_CALLER_APP);
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            String path = request.getRequestURI();
            if (!path.startsWith(ACTUATOR_PREFIX)) {
                DigestLoggers.emit(DigestLoggers.RPC_SERVER, DigestLoggers.fields(
                        "span", "server",
                        "method", request.getMethod(),
                        "path", path,
                        "status", response.getStatus(),
                        "costMs", cost,
                        "remote.app", caller == null ? "external" : caller));
            }
            TraceContext.clear();
        }
    }
}
