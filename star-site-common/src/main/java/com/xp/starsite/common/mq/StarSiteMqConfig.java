package com.xp.starsite.common.mq;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * MQ 封装配装入口：业务服务显式 @Import(StarSiteMqConfig.class) 才生效。
 * <p>
 * common 模块不含 @Component（启动类 scanBasePackages 扫全 starsite 包，
 * 裸组件会污染不使用 MQ 的服务如网关），故用显式导入 + 条件装配：
 * 配置 rocketmq.name-server 后注册 RocketMqEventPublisher；
 * 未配置时降级注册 NoopEventPublisher（健康聚焦阶段 MQ 停用，业务注入 EventPublisher
 * 的构造依赖仍可满足，发布为空操作），服务可独立于 MQ 容器启停。
 * <p>
 * 注意：本类会被无 rocketmq 依赖的服务（health/member/family 等）在组件扫描时
 * introspect，方法签名不得出现 rocketmq 类型（只在方法体内 new，条件成立才执行）；
 * 配置一律从 Environment 读，不在签名里引 RocketMQProperties。
 */
@Configuration
public class StarSiteMqConfig {

    @Bean(destroyMethod = "destroy")
    @ConditionalOnProperty("rocketmq.name-server")
    public EventPublisher eventPublisher(Environment env) {
        String namesrv = env.getProperty("rocketmq.name-server");
        String group = env.getProperty("rocketmq.producer.group",
                "G_" + env.getProperty("spring.application.name", "app") + "_producer");
        return new RocketMqEventPublisher(namesrv, group,
                env.getProperty("spring.application.name", "unknown"));
    }

    // 声明在真实实现之后：@ConditionalOnMissingBean 按注册顺序求值
    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher noopEventPublisher() {
        return new NoopEventPublisher();
    }
}
