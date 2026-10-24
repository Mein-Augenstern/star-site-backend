package com.xp.starsite.reminder.config;

import com.xp.starsite.common.util.JwtUtil;
import com.xp.starsite.common.web.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：/api/** 需 JWT；/internal/** 仅内网（网关无路由，外网 404）
 *
 * @author muyi
 * @since 2026-10
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;


    /** 归属校验门卫（FamilyApi 显式装配，供用户自建提醒规则使用） */
    @Bean
    public com.xp.starsite.api.client.FamilyOwnershipGuard familyOwnershipGuard(com.xp.starsite.api.client.FamilyApi familyApi) {
        return new com.xp.starsite.api.client.FamilyOwnershipGuard(familyApi);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtAuthInterceptor(jwtUtil))
                .addPathPatterns("/api/**");
    }
}
