package com.xp.starsite.archive.config;

import com.xp.starsite.api.client.FamilyApi;
import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.common.util.JwtUtil;
import com.xp.starsite.common.web.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：全部接口需要 JWT（CORS 统一在网关处理）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;

    /** 归属校验门卫：包装 Feign 调 family 服务（Guard 故意无 @Component，这里显式装配） */
    @Bean
    public FamilyOwnershipGuard familyOwnershipGuard(FamilyApi familyApi) {
        return new FamilyOwnershipGuard(familyApi);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtAuthInterceptor(jwtUtil))
                .addPathPatterns("/api/**");
    }
}
