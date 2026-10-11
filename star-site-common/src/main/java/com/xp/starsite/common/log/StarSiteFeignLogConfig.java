package com.xp.starsite.common.log;

import feign.Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Feign 出站留痕装配：仅当类路径存在 feign（即服务真用了 OpenFeign）才注册。
 * <p>
 * 必须用类级 @ConditionalOnClass（而非方法级）：Spring 解析配置类时会内省全部 @Bean 方法签名，
 * 方法级条件挡不住对 feign.Client 类型本身的装载——无 feign 的服务（如 member）会直接
 * NoClassDefFoundError。类级条件让整个配置类在无 feign 时不参与解析。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Configuration
@ConditionalOnClass(Client.class)
public class StarSiteFeignLogConfig {

    @Bean
    public FeignTraceInterceptor feignTraceInterceptor(Environment env) {
        return new FeignTraceInterceptor(env.getProperty("spring.application.name", "unknown"));
    }

    @Bean
    public FeignDigestClient feignDigestClient(Client defaultClient, Environment env) {
        return new FeignDigestClient(defaultClient, env.getProperty("spring.application.name", "unknown"));
    }
}
