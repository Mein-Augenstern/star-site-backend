package com.xp.starsite.finance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

/**
 * 财务微服务启动类（F1：账户 + 记账；F3 接入账单导入时再引 MQ/Feign）
 *
 * @author muyi
 * @since 2026-10
 */
@SpringBootApplication(scanBasePackages = "com.xp.starsite")
@EnableDiscoveryClient
@EnableScheduling
// 资产负债表聚合调 archive 服务（ArchiveApi 契约在 star-site-api）
// 跨语言 AI 契约在消费方 integration 模块（docs/技术/12 §跨语言规约），一并纳入扫描
@EnableFeignClients(basePackages = {"com.xp.starsite.api.client", "com.xp.starsite.finance.ai"})
// 消费 TOPIC_FILE（账单导入事件）：显式装配，未配 rocketmq.name-server 时不生效
@Import(com.xp.starsite.common.mq.StarSiteMqConfig.class)
@MapperScan("com.xp.starsite.finance.mapper")
public class FinanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceServiceApplication.class, args);
    }
}
