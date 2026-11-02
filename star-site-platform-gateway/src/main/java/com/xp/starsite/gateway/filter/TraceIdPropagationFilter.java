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

import com.xp.starsite.common.log.TraceContext;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * traceId 下游传播过滤器（日志巡检 P1 修复 2026-11-02）。
 *
 * <p><b>为什么需要单独的 Filter</b>：GatewayDigestWebFilter(-200) 的
 * {@code exchange.mutate()} 在 LoadBalancer 过滤器链(order=10150)中被丢弃——
 * 日志巡检实证：网关 digest 有 traceId 但 member 自建新 ID（两种 mutate 写法
 * 均不生效，member 侧 0 次匹配网关 traceId）。
 *
 * <p><b>方案</b>：本 Filter 在 LoadBalancer(10150) 之后、NettyRouting(MAX) 之前
 * 执行（order=10200），从 exchange attribute 读 GatewayDigestWebFilter 生成的
 * traceId，注入下游请求头。此时 LoadBalancer 已完成服务发现，本阶段的 mutate
 * 直接作用于 Netty 发出的 HTTP 请求。
 *
 * @author muyi
 * @since 2026-11
 */
@Component
public class TraceIdPropagationFilter implements GlobalFilter, Ordered {

    /**
     * 执行顺序 10200：LoadBalancer(10150) 之后、NettyRouting(MAX) 之前。
     * 必须在 LoadBalancer 之后——否则 mutate 的 request 会被 LB 覆盖。
     */
    @Override
    public int getOrder() {
        return 10200;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getAttribute(GatewayDigestWebFilter.ATTR_TRACE_ID);
        if (traceId != null && !traceId.isBlank()) {
            var mutatedRequest = exchange.getRequest().mutate()
                    .header(TraceContext.HEADER_TRACE_ID, traceId)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }
        return chain.filter(exchange);
    }
}
