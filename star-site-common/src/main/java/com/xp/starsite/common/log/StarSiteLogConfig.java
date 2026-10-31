package com.xp.starsite.common.log;

import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
import lombok.extern.slf4j.Slf4j;

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
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class StarSiteLogConfig {

    /** digest 行的 app 字段来源（spring.application.name）——构造注入（@PostConstruct 方法不可带参） */
    public StarSiteLogConfig(Environment env) {
        DigestLoggers.APP_NAME = env.getProperty("spring.application.name", "unknown");
    }

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

    /**
     * 全局 ID（物理主键）通用组件：引擎已迁移至 starsite-sequence 组件
     * （com.xp.starsite:starsite-sequence 的 generator/facade/allocator 分包），
     * 本方法注册组件门面 Bean（starsite-async-command 等组件 starter 按类型取用），
     * 并保留 backend 侧的分配策略编排（部署决策不进组件默认装配）：
     * 每个服务在 sys_sequence 表中有各自的一行 sequence（隔离键=服务名），
     * 启动时原子分配 workerId——根治全平台共享 workerId=1 的跨服务撞号问题。
     * <ul>
     *   <li>显式配置 star.snowflake.worker-id 优先（单测/特殊部署确定性用）；</li>
     *   <li>无 DB（理论兜底）或租约失败：回落 hash(服务名)%1024（按服务确定，降低撞号面）；</li>
     *   <li>sequence 表缺失自动创建（IF NOT EXISTS），DDL 同步在 docker/mysql-init/22-sys-sequence.sql。</li>
     * </ul>
     * 需要应用侧生成 id 的场景经业务层显式调用 nextId()（替代 MyBatis-Plus ASSIGN_ID）。
     */
    @Bean
    public com.xp.starsite.sequence.facade.StarsiteIdGenerator starsiteIdGenerator(
            Environment env, ObjectProvider<DataSource> dataSourceProvider) {
        String appName = env.getProperty("spring.application.name", "unknown");
        long workerId = resolveWorkerId(appName, env, dataSourceProvider);
        return new com.xp.starsite.sequence.facade.StarsiteIdGenerator(
                new com.xp.starsite.sequence.generator.SnowflakeIdGenerator(
                        workerId, 1767225600000L, 10, 12, 2000,
                        com.xp.starsite.sequence.generator.SnowflakeIdGenerator.BackwardPolicy.CLAMP),
                workerId, "mysql-lease(" + appName + ")");
    }

    /** 兼容门面 Bean：保留 backend 原 IdGenerator API（nextId()），内部委托组件门面（同一引擎实例） */
    @Bean
    public com.xp.starsite.common.util.IdGenerator idGenerator(
            com.xp.starsite.sequence.facade.StarsiteIdGenerator starsiteIdGenerator) {
        return new com.xp.starsite.common.util.IdGenerator(starsiteIdGenerator);
    }

    /** 机器位分配策略：显式配置 → sequence 表租约 → hash 回落 */
    private long resolveWorkerId(String appName, Environment env,
                                 ObjectProvider<DataSource> dataSourceProvider) {
        Long explicit = env.getProperty("star.snowflake.worker-id", Long.class);
        if (explicit != null) {
            log.info("[id] workerId 使用显式配置: service={}, workerId={}", appName, explicit);
            return explicit;
        }
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            log.warn("[id] 无可用 DataSource，workerId 回落 hash: service={}", appName);
            return fallbackWorkerId(appName);
        }
        try {
            // 组件的 MySQL 租约分配器（__global__ 池 + LAST_INSERT_ID 原子取号，语义与原自研一致）
            long workerId = new com.xp.starsite.sequence.allocator.MysqlWorkerIdAllocator(
                    dataSource, "sys_sequence", appName,
                    appName + "@" + env.getProperty("PID", "0") + "@" + System.currentTimeMillis(),
                    true, 10,
                    com.xp.starsite.sequence.allocator.MysqlWorkerIdAllocator.WraparoundPolicy.RESET).allocate();
            log.info("[id] workerId 经 sequence 表分配（starsite-sequence 租约）: service={}, workerId={}", appName, workerId);
            return workerId;
        } catch (Exception e) {
            long fallback = fallbackWorkerId(appName);
            // ERROR 而非 WARN（BUG-009 实证）：租约失败常因 sys_sequence 结构陈旧（旧 volume 无 id 列），
            // 回落 hash 只是降级不是修复——跨服务撞号风险必须醒目可见，消息直接给排查动作
            log.error("[id] workerId sequence 分配失败，回落 hash({}): service={}, err={}"
                            + "（排查：DESC sys_sequence 是否含 id/gmt_* 列——旧库需按 docker/mysql-init/22-sys-sequence.sql 重建；"
                            + "组件 0.2.1+ 的结构校验会在启动期给出明确报错）",
                    fallback, appName, e.getMessage());
            return fallback;
        }
    }

    /** 兜底机器位：按服务名 hash（同服务确定、跨服务离散），把撞号面从「全平台同一值」缩到「同服务多实例」 */
    private long fallbackWorkerId(String appName) {
        return Math.floorMod(appName.hashCode(), 1024);
    }
}
