package com.xp.starsite.health;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 体检报告微服务启动类
 *
 * @author muyi
 * @since 2026-10
 */
@SpringBootApplication(scanBasePackages = "com.xp.starsite")
@EnableDiscoveryClient
// health 自有 @Scheduled 扫描任务与组件调度线程并存（组件自带调度，无需 @EnableScheduling 供其使用）
@EnableScheduling
// 归属校验调 family 服务（FamilyApi 契约在 star-site-api）
// 跨语言 AI 契约在消费方 integration 模块（docs/技术/12 §跨语言规约），一并纳入扫描
@EnableFeignClients(basePackages = {"com.xp.starsite.api.client", "com.xp.starsite.health.integration.ai"})
// starsite-async-command 组件经 starter 自动装配（starsite.async-command.enabled，默认 true）
@MapperScan("com.xp.starsite.health.mapper")
public class HealthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthServiceApplication.class, args);
    }
}
