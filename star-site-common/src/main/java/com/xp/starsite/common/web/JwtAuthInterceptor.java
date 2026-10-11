package com.xp.starsite.common.web;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 可复用的 MVC JWT 拦截器（业务服务在 WebConfig 里注册为 @Bean 使用；
 * 故意不加 @Component，避免被 WebFlux 网关的组件扫描误加载）
 *
 * @author xiaopu
 * @since 2026-10
 */
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String header = request.getHeader(AuthConstants.AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(AuthConstants.BEARER_PREFIX)) {
            String token = header.substring(AuthConstants.BEARER_PREFIX.length());
            try {
                Claims claims = jwtUtil.parseToken(token);
                request.setAttribute(AuthConstants.CLAIM_MEMBER_ID, Long.valueOf(claims.getSubject()));
                request.setAttribute(AuthConstants.CLAIM_USERNAME, claims.get(AuthConstants.CLAIM_USERNAME, String.class));
                return true;
            } catch (Exception ignored) {
                // token 非法/过期，走下方 401
            }
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
        return false;
    }
}
