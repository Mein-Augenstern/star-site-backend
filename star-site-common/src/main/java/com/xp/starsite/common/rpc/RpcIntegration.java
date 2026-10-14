package com.xp.starsite.common.rpc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC 集成出口标记：标注在 integration 模块的跨服务调用类上。
 * <p>
 * 三个作用：① 启动期 {@link RpcDependencyChecker} 据此扫描并校验类内 Feign 客户端
 * 字段注入完整（缺依赖 fail-fast）；② 输出本服务 RPC 依赖面清单（服务 → 依赖服务）；
 * ③ 代码自识别——看到本注解即知「此类是跨服务出口，调用必须经 RpcCallGuard」。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcIntegration {

    /** 依赖的目标服务名（spring.application.name），自文档用，如 "star-site-platform-reminder" */
    String[] value() default {};
}
