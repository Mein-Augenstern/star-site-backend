package com.xp.starsite.common.async;

import com.xp.starsite.common.async.mapper.AsyncCommandMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * star-async 装配与调度（对应 asyncmd 的 elasticjob 角色，本组件用 DB 轮询替代 ZK 分片）。
 * <p>
 * 刻意【无 @Configuration 注解】：消费服务显式 @Import(AsyncCommandConfig.class) 才生效
 * （与 StarSiteMqConfig 同款模式）——网关等不导入的服务在组件扫描时不会把本类当配置类反射，
 * 从而隔离 mybatis 类型缺失问题（common 无 mybatis 运行时依赖，provided 仅编译期）。
 * <p>
 * 按服务开关：环境变量 STAR_ASYNC_ENABLED=true。消费服务还需 @EnableScheduling。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@ConditionalOnProperty(name = "star.async.enabled", havingValue = "true")
// mapper 独占子包（common.async.mapper）：包即扫描边界，同包 Executor 接口不会被误注册
@org.mybatis.spring.annotation.MapperScan(basePackages = "com.xp.starsite.common.async.mapper")
public class AsyncCommandConfig {

    /** 管理员白名单（逗号分隔 memberId；未配置=放行，本地开发态） */
    private final String adminMemberIds;

    /** 管控端点基路径（每服务独立 DB 后各服务自持表，网关按路径前缀路由到对应服务） */
    private final String adminBasePath;

    public AsyncCommandConfig(Environment env) {
        this.adminMemberIds = env.getProperty("star.async.admin-member-ids", "");
        this.adminBasePath = env.getProperty("star.async.admin-base-path", "/api/admin/async");
    }

    @Bean
    public AsyncCommandService asyncCommandService(AsyncCommandMapper mapper) {
        return new AsyncCommandService(mapper);
    }

    /** 建表初始化：async_command 表随组件走（每服务独立 DB 后各服务自持，增量安全） */
    @Bean
    public AsyncCommandDdlInitializer asyncCommandDdlInitializer(org.springframework.beans.factory.ObjectProvider<javax.sql.DataSource> dataSourceProvider) {
        return new AsyncCommandDdlInitializer(dataSourceProvider.getIfAvailable());
    }

    @Bean
    public AsyncCommandAdminService asyncCommandAdminService(AsyncCommandService commandService) {
        return new AsyncCommandAdminService(commandService, adminMemberIds);
    }

    @Slf4j
    @RequiredArgsConstructor
    static class Scheduler {

        private final AsyncCommandMapper mapper;
        private final AsyncCommandService service;
        private final ObjectProvider<List<AsyncCommandExecutor>> executors;
        private Map<String, AsyncCommandExecutor> routes;

        private Map<String, AsyncCommandExecutor> routes() {
            if (routes == null) {
                List<AsyncCommandExecutor> list = executors.getIfAvailable() == null
                        ? List.of() : executors.getIfAvailable();
                routes = list.stream().collect(Collectors.toMap(AsyncCommandExecutor::type, e -> e));
                log.info("star-async executors registered: {}", routes.keySet());
            }
            return routes;
        }

        /** 认领并执行到期命令（每 5s）：令牌法认领 → 按 cmd_type 路由 → 成败落状态 */
        @Scheduled(fixedDelay = 5000)
        public void tick() {
            int reclaimed = mapper.reclaimStale(10);
            if (reclaimed > 0) {
                log.warn("star-async reclaimed {} stale RUNNING cmds", reclaimed);
            }
            String token = java.util.UUID.randomUUID().toString();
            if (mapper.claim(token, 10) == 0) {
                return;
            }
            for (AsyncCommand cmd : mapper.findByToken(token)) {
                AsyncCommandExecutor executor = routes().get(cmd.getCmdType());
                if (executor == null) {
                    service.markRetryOrFail(cmd,
                            new IllegalStateException("无注册执行器: " + cmd.getCmdType()));
                    continue;
                }
                try {
                    executor.execute(cmd);
                    service.markSuccess(cmd);
                    log.info("async cmd done: id={}, type={}", cmd.getId(), cmd.getCmdType());
                } catch (Exception e) {
                    service.markRetryOrFail(cmd, e);
                }
            }
        }
    }

    @Bean
    public Scheduler asyncCommandScheduler(AsyncCommandMapper mapper,
                                           AsyncCommandService service,
                                           ObjectProvider<List<AsyncCommandExecutor>> executors) {
        return new Scheduler(mapper, service, executors);
    }
}
