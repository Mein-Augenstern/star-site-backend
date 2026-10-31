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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 全局 JWT 校验过滤器：白名单外一律要求 Authorization: Bearer <token>。
 * 校验通过后原样转发（下游服务各自保留拦截器做纵深防御）。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** 免 token 白名单（/api/payment/notify/** 为渠道服务器异步回调，防线是渠道验签；
     *  /api/bench/** 为三组件压测端点——仅本地/内网开闸，公网部署须移除该路由或加访问控制） */
    private static final String[] WHITELIST = {
            "/api/auth/**",
            "/api/payment/notify/**",
            "/api/bench/**",
            "/actuator/**",
    };

    private final JwtUtil jwtUtil;

    @Override
    public int getOrder() {
        return -100;
    }

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
                jwtUtil.parseToken(token);
                return chain.filter(exchange);
            } catch (Exception e) {
                log.debug("网关拒绝无效 token: path={}, err={}", path, e.getMessage());
            }
        }
        return unauthorized(exchange);
    }

    private boolean isWhitelisted(String path) {
        for (String pattern : WHITELIST) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
