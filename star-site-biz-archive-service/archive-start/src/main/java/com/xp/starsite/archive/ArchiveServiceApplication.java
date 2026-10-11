package com.xp.starsite.archive;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

/**
 * 家庭档案微服务启动类（证件 + 资产两个小域合署）
 *
 * @author xiaopu
 * @since 2026-10
 */
@SpringBootApplication(scanBasePackages = "com.xp.starsite")
@EnableDiscoveryClient
// 归属校验调 family 服务（FamilyApi 契约在 star-site-api）
@EnableFeignClients(basePackages = "com.xp.starsite.api.client")
// MQ 显式装配（common 无裸组件；未配 rocketmq.name-server 时不生效）
@Import(com.xp.starsite.common.mq.StarSiteMqConfig.class)
@MapperScan("com.xp.starsite.archive.mapper")
public class ArchiveServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArchiveServiceApplication.class, args);
    }
}
