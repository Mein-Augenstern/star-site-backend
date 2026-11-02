/*
 * Copyright 2026 muyi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xp.starsite.gateway.filter;

import com.xp.starsite.common.log.DigestLoggers;
import com.xp.starsite.common.log.TraceContext;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关入口 digest + traceId 分发（WebFlux 形态，日志巡检 P1 修复 2026-11-01）。
 *
 * <p><b>为什么网关需要自己的 Filter</b>：common 的 RpcServerDigestFilter 是 Servlet
 * （OncePerRequestFilter），只在 Tomcat 体系注册——网关是 WebFlux/Netty，Servlet Filter
 * 根本不会生效，导致 digest 八文件体系恰在流量入口成为盲区（gateway 容器内
 * rpc-server-digest.log 0 字节，日志巡检实证）。
 *
 * <p><b>职责</b>（对齐 Servlet 版语义， span=server）：
 * <ol>
 *   <li><b>traceId 入口生成</b>：无 X-Trace-Id 头则生成 UUID（网关是全链路起点，
 *       后端服务经 TraceContext.set 从头里继承）——解决"traceId 跨服务断裂"：
 *       此前网关日志空 trace 段、下游各服务 traceId 各自独立，排障时链路在入口断开</li>
 *   <li><b>向下透传</b>：X-Trace-Id + X-Caller-App（值=目标路由 id）写入下游请求头，
 *       下游 Java 服务 TraceContext 继承、invest（Python）middleware 读取</li>
 *   <li><b>rpc-server digest</b>：method/path/status/costMs/route（span=server，对齐
 *       DigestLoggers 八文件语义），写入 logs/{app}/rpc-server-digest.log 供日志巡检</li>
 *   <li><b>MDC</b>：WebFlux 的 Reactor 线程模型使 MDC 不可靠（线程切换丢上下文），
 *       traceId 经 exchange attributes 传递而非 MDC——网关日志行中 traceId 以
 *       exchange attr 为准（本 Filter 置入，ExceptionHandler 引用）</li>
 * </ol>
 *
 * <p>探针端点（/actuator/**）不打 digest 防噪音——与 Servlet 版一致。
 *
 * @author muyi
 * @since 2026-11
 */
@Component
public class GatewayDigestWebFilter implements GlobalFilter, Ordered {

    /** 探针端点不打 digest（健康检查噪音）——与 Servlet 版 RpcServerDigestFilter 一致 */
    private static final String ACTUATOR_PREFIX = "/actuator";

    /** exchange attr key：本请求的 traceId（MDC 在 Reactor 下不可靠，以 attr 为准） */
    public static final String ATTR_TRACE_ID = "starsite.traceId";

    /** 执行顺序：最先执行（traceId 要在鉴权/路由之前生成，否则 JwtGlobalFilter 的 401 日志无 trace） */
    @Override
    public int getOrder() {
        return -200; // JwtGlobalFilter 是 -100，digest 先于鉴权
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        String path = exchange.getRequest().getPath().value();

        // ① traceId：外部带入则继承（可能前面还有 LB/网关），否则入口生成
        // final：lambda 内引用需 effectively final（编译器约束，非设计语义）
        final String traceId;
        String incoming = exchange.getRequest().getHeaders().getFirst(TraceContext.HEADER_TRACE_ID);
        traceId = (incoming == null || incoming.isBlank())
                ? UUID.randomUUID().toString().replace("-", "")
                : incoming;
        exchange.getAttributes().put(ATTR_TRACE_ID, traceId);

        // ② 向下游透传：X-Trace-Id + X-Caller-App（值=本网关标识，下游知道"谁调的我"）
        // 用 request.mutate().build() 两步构建——exchange.mutate(request -> headers) 在
        // LoadBalancer 过滤器链中被覆盖（日志巡检实证：网关 digest 有 traceId 但 member 自建新 ID）。
        // 改为直接 mutate ServerHttpRequest → 再 mutate exchange，确保 header 在 routing filter 可见。
        final String routeId = resolveRouteId(exchange);
        var mutatedRequest = exchange.getRequest().mutate()
                .header(TraceContext.HEADER_TRACE_ID, traceId)
                .header(TraceContext.HEADER_CALLER_APP, "gateway:" + routeId)
                .build();
        var mutated = exchange.mutate().request(mutatedRequest).build();

        // ③ digest：请求完成时打（span=server 对齐 Servlet 版语义；route 字段网关特有）
        return chain.filter(mutated).doFinally(signal -> {
            if (!path.startsWith(ACTUATOR_PREFIX)) {
                long cost = System.currentTimeMillis() - start;
                var response = mutated.getResponse();
                int status = response.getStatusCode() != null ? response.getStatusCode().value() : -1;
                DigestLoggers.emit(DigestLoggers.RPC_SERVER, DigestLoggers.fields(
                        "span", "server",
                        "method", mutated.getRequest().getMethod().name(),
                        "path", path,
                        "status", status,
                        "costMs", cost,
                        "route", routeId,
                        "remote.app", "external"));
            }
        });
    }

    /**
     * 解析当前命中的路由 id（日志字段用；未匹配路由时 = "unmatched"）。
     *
     * @param exchange 当前交换
     * @return 路由 id 或 "unmatched"
     */
    private String resolveRouteId(ServerWebExchange exchange) {
        var route = exchange.getAttribute("org.springframework.cloud.gateway.support.ServerWebExchangeUtils.gatewayRoute");
        return route != null ? route.toString() : "unmatched";
    }
}
