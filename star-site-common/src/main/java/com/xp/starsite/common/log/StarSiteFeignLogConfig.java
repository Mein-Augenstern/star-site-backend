package com.xp.starsite.common.log;

import feign.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Feign 出站留痕装配：仅当类路径存在 feign（即服务真用了 OpenFeign）才注册。
 * <p>
 * 必须用类级 @ConditionalOnClass（而非方法级）：Spring 解析配置类时会内省全部 @Bean 方法签名，
 * 方法级条件挡不住对 feign.Client 类型本身的装载——无 feign 的服务（如 member）会直接
 * NoClassDefFoundError。类级条件让整个配置类在无 feign 时不参与解析。
 * <p>
 * 出站留痕经 {@link FeignDigestLogger}（feign.Logger 扩展点，与 Client 类型无交集，
 * 规避「@Primary Client 注入自身」的循环依赖）；logger-level/BASIC 由本类程序化兜底
 * （env 带连字符的键映射 relaxed binding 易错，代码默认值零歧义，yml 仍可覆盖）。
 * 另有 {@link FeignTraceInterceptor} 透传 traceId/X-Caller-App。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Configuration
@ConditionalOnClass(Logger.class)
public class StarSiteFeignLogConfig {

    /** 默认基线：BASIC 激活 Logger 回调 + 间调用超时（yml/env 显式配置可覆盖） */
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 3000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 10000;

    public StarSiteFeignLogConfig(FeignClientProperties props) {
        FeignClientProperties.FeignClientConfiguration cfg = props.getConfig()
                .computeIfAbsent("default", k -> new FeignClientProperties.FeignClientConfiguration());
        if (cfg.getLoggerLevel() == null) {
            cfg.setLoggerLevel(Logger.Level.BASIC);
        }
        if (cfg.getConnectTimeout() == null) {
            cfg.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MS);
        }
        if (cfg.getReadTimeout() == null) {
            cfg.setReadTimeout(DEFAULT_READ_TIMEOUT_MS);
        }
    }

    /** 全部 Feign 客户端共享的 digest 日志器（Spring Cloud OpenFeign 自动套用到每个客户端） */
    @Bean
    public Logger feignDigestLogger() {
        return new FeignDigestLogger();
    }

    /** 出站头透传：traceId + 调用方应用名（服务端留痕的「谁调的我」来源） */
    @Bean
    public FeignTraceInterceptor feignTraceInterceptor(Environment env) {
        return new FeignTraceInterceptor(env.getProperty("spring.application.name", "unknown"));
    }
}
