package com.xp.starsite.common.log;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import jakarta.servlet.DispatcherType;
import org.apache.ibatis.plugin.Interceptor;

import java.util.EnumSet;

import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;

/**
 * 日志留痕体系装配（servlet 服务自动生效，gateway/WebFlux 零感知）：
 * <ul>
 *   <li>{@link RpcServerDigestFilter}——服务端请求留痕，最高优先级（先于一切业务过滤器建立 traceId）</li>
 *   <li>{@link DbDigestInterceptor}——SQL 留痕（有 mybatis 类路径才装）</li>
 *   <li>{@link QueryTemplate}/{@link WriteTemplate}——统一业务模板读写双变体</li>
 *   <li>Feign 出站留痕见 {@link StarSiteFeignLogConfig}（类级条件，无 feign 的服务不解析）</li>
 * </ul>
 * 配置类本身随 scanBasePackages 扫描注册，条件注解保证网关/WebFlux 不受影响；
 * digest 日志文件路径由各服务 logback-spring.xml 定义（SOFA 命名惯例）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class StarSiteLogConfig {

    @Bean
    public FilterRegistrationBean<RpcServerDigestFilter> rpcServerDigestFilter() {
        FilterRegistrationBean<RpcServerDigestFilter> reg = new FilterRegistrationBean<>(new RpcServerDigestFilter());
        reg.setName("rpcServerDigestFilter");
        // 最高优先级：traceId 必须先于鉴权/业务过滤器建立
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/*");
        reg.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        return reg;
    }

    @Bean
    @ConditionalOnClass(name = "org.apache.ibatis.session.SqlSessionFactory")
    public Interceptor dbDigestInterceptor(Environment env) {
        // 阈值配置化：star.log.db.slow-sql-ms / star.log.db.max-sql-length
        return new DbDigestInterceptor(
                env.getProperty("star.log.db.slow-sql-ms", Long.class, 1000L),
                env.getProperty("star.log.db.max-sql-length", Integer.class, 500));
    }

    @Bean
    public QueryTemplate queryTemplate(Environment env) {
        return new QueryTemplate(env.getProperty("spring.application.name", "unknown"));
    }

    @Bean
    public WriteTemplate writeTemplate(Environment env) {
        return new WriteTemplate(env.getProperty("spring.application.name", "unknown"));
    }

    /** 雪花 id（替代 MyBatis-Plus ASSIGN_ID）：需要 insert 前拿 id 的场景经业务层显式调用 */
    @Bean
    public com.xp.starsite.common.util.IdGenerator idGenerator(Environment env) {
        return new com.xp.starsite.common.util.IdGenerator(
                env.getProperty("star.snowflake.worker-id", Long.class, 1L));
    }
}
