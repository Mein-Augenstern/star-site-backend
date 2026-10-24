package com.xp.starsite.common.rpc;

import feign.Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RPC 集成基建装配：仅当类路径存在 feign（服务真用了 OpenFeign）才注册。
 * <p>
 * 类级 @ConditionalOnClass 蓝本同 {@code StarSiteFeignLogConfig}——方法级条件挡不住
 * 配置类解析期的签名内省，无 feign 的服务（family/file/member/网关）必须整体跳过本类。
 * 注册两件套：{@link RpcCallGuard}（调用门卫，integration 与 FamilyOwnershipGuard 消费）
 * 与 {@link RpcDependencyChecker}（启动期依赖注入校验）。
 * 超时基线见 StarSiteFeignLogConfig（star.rpc.timeout.* 可配置）。
 *
 * @author muyi
 * @since 2026-10
 */
@Configuration
@ConditionalOnClass(Client.class)
public class StarSiteRpcConfig {

    @Bean
    public RpcCallGuard rpcCallGuard() {
        return new RpcCallGuard();
    }

    @Bean
    public RpcDependencyChecker rpcDependencyChecker(ApplicationContext applicationContext) {
        return new RpcDependencyChecker(applicationContext);
    }
}
