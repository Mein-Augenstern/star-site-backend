package com.xiaopu.starsite.member.interceptor;

import com.xiaopu.starsite.common.constants.AuthConstants;
import com.xiaopu.starsite.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT 校验拦截器：校验 Authorization: Bearer <token>，
 * 通过后把 memberId / username 放入 request attribute
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

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
