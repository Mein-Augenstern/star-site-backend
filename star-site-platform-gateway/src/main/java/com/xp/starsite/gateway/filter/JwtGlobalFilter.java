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

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;

/**
 * 全局 JWT 校验过滤器（网关唯一鉴权关卡，WebFlux/Netty 形态）。
 *
 * <p><b>鉴权语义</b>：白名单外一律要求 {@code Authorization: Bearer <token>}；
 * 校验通过后<b>原样转发</b>——token 不解构、角色不判断，下游服务各自保留拦截器做
 * <b>纵深防御</b>（member 再验一次签名+过期；invest star 模式也独立验 JWT——
 * G2 公网化三端密钥对齐后实证全链路通）。
 *
 * <p><b>为什么不在此提取角色/用户信息</b>：网关只做"有没有票"，"票是什么座位"
 * 由各服务自行解析（服务可能对同一 token 有不同的角色映射——如 admin 在
 * member 是管理端、在 health 只是普通用户——网关统一提取反而耦合业务）。
 *
 * <p><b>白名单策略</b>（新增白名单须注明理由+防线，这是安全评审的硬要求）：
 * <ul>
 *   <li>{@code /api/auth/**}——登录/注册本身（还没 token 可验）</li>
 *   <li>{@code /api/payment/notify/**}——渠道服务器异步回调（防线=渠道 HMAC 验签，
 *       这是支付回调的行业标准做法，验签在 payment-service 内部完成）</li>
 *   <li>{@code /api/bench/**}——三组件压测端点（<b>公网部署须移除该路由或加 IP 白名单</b>；
 *       本地/内网开发环境开闸，G2 验收用例 A 组依赖）</li>
 *   <li>{@code /actuator/**}——K8s 探针（health/readiness 不能要求 token，
 *       否则 Pod 被误判不健康连环重启——故障注入 OBS-108 实证）</li>
 * </ul>
 *
 * <p><b>执行顺序</b>（{@link #getOrder() getOrder} = -100）：
 * {@code GatewayDigestWebFilter(-200) → 本 Filter(-100) → 路由转发}——
 * digest 先于鉴权（否则 401 请求无 traceId，日志巡检时排障链路在入口断开）。
 *
 * <p><b>性能</b>：纯对称验签（HS256/384，JwtUtil 内部 jjwt），无网络调用；
 * WebFlux 非阻塞——parseToken 同步但耗时 &lt;1ms（压测实证不构成瓶颈）。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    /** Ant 风格路径匹配器（白名单模式匹配用；Spring 自带，线程安全） */
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /**
     * 免 token 白名单（新增条目必须注明：谁能调、防线是什么——否则安全审计不通过）。
     * <ul>
     *   <li>{@code /api/auth/**}——登录/注册（无 token 可验；防线=接口自身限流/验证码）</li>
     *   <li>{@code /api/payment/notify/**}——渠道异步回调（防线=HMAC 验签 in payment-service）</li>
     *   <li>{@code /api/bench/**}——三组件压测端点（公网部署须移除或加 IP 白名单；
     *       本地开发环境开闸，G2 验收 A 组依赖）</li>
     *   <li>{@code /actuator/**}——K8s 探针（health 不应要求 token，
     *       否则 Pod 连环重启——故障注入 OBS-108 实证）</li>
     * </ul>
     */
    private static final String[] WHITELIST = {
            "/api/auth/**",
            "/api/payment/notify/**",
            "/api/bench/**",
            "/actuator/**",
    };

    /** JWT 工具（common 提供；HS256/384 对称验签，密钥经 JWT_SECRET 环境变量注入） */
    private final JwtUtil jwtUtil;

    /**
     * 执行顺序 -100：在路由转发前、在 GatewayDigestWebFilter(-200) 后。
     * 顺序约束：digest 先行（保证 401 也有 traceId），路由最后。
     */
    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * {@inheritDoc}
     *
     * <p>流程：白名单命中直接放行 → 取 Authorization 头（Bearer 前缀）→
     * parseToken 验签+过期检查 → 通过则原样转发（token 留给下游做纵深防御）→
     * 不通过返回 401 JSON。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(AuthConstants.AUTH_HEADER);
        if (header != null && header.startsWith(AuthConstants.BEARER_PREFIX)) {
            String token = header.substring(AuthConstants.BEARER_PREFIX.length());
            try {
                // 验签+过期（jjwt 抛异常即拒绝；异常类型不区分——对调用方统一 401 防探测）
                jwtUtil.parseToken(token);
                return chain.filter(exchange);
            } catch (Exception e) {
                // debug 级：无效 token 是常态噪音（用户 token 过期、攻击扫描），INFO 会刷爆日志
                log.debug("网关拒绝无效 token: path={}, err={}", path, e.getMessage());
            }
        }
        return unauthorized(exchange);
    }

    /**
     * 判断请求路径是否命中白名单（Ant 模式匹配：** 跨多级路径）。
     *
     * @param path 请求路径（不含 query string）
     * @return true=白名单命中（跳过 JWT 校验直接放行）
     */
    private boolean isWhitelisted(String path) {
        for (String pattern : WHITELIST) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构造 401 响应（统一 JSON 结构，前端按 code=401 触发"登录已过期"自愈流程）。
     *
     * <p>为什么不返回 403：401=未认证（缺/过期 token，引导用户重新登录），
     * 403=已认证但无权限（引导用户"联系管理员"）——此处是前者。
     *
     * @param exchange 当前交换（响应写入用）
     * @return 写入完成的 Mono（WebFlux 响应式写入，非阻塞）
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
