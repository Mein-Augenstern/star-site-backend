package com.xp.starsite.payment.config;

import com.xp.starsite.common.util.JwtUtil;
import com.xp.starsite.common.web.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：/api/** 全部需要 JWT，渠道回调 /api/payment/notify/** 例外
 * （外部渠道服务器无 token，唯一防线是渠道验签；CORS 统一在网关处理）。
 *
 * @author muyi
 * @since 2026-10
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtAuthInterceptor(jwtUtil))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/payment/notify/**");
    }
}
