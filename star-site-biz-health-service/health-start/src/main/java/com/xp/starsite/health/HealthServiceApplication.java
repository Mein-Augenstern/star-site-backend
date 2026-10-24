package com.xp.starsite.health;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import com.xp.starsite.common.async.AsyncCommandConfig;

/**
 * 体检报告微服务启动类
 *
 * @author muyi
 * @since 2026-10
 */
@SpringBootApplication(scanBasePackages = "com.xp.starsite")
@EnableDiscoveryClient
// star-async 异步命令调度（STAR_ASYNC_ENABLED=true 启用组件）
@EnableScheduling
// 归属校验调 family 服务（FamilyApi 契约在 star-site-api）
// 跨语言 AI 契约在消费方 integration 模块（docs/技术/12 §跨语言规约），一并纳入扫描
@EnableFeignClients(basePackages = {"com.xp.starsite.api.client", "com.xp.starsite.health.integration.ai"})
// star-async 异步命令组件：显式导入（无注解配置类，网关不导入则零感知）
@Import(AsyncCommandConfig.class)
@MapperScan("com.xp.starsite.health.mapper")
public class HealthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthServiceApplication.class, args);
    }
}
