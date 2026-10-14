package com.xp.starsite.recheck;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 复查闭环微服务启动类（垂直健康产品样板：依赖 health/reminder/family 横向服务）
 *
 * @author xiaopu
 * @since 2026-10
 */
@SpringBootApplication(scanBasePackages = "com.xp.starsite")
@EnableDiscoveryClient
// 归属校验调 family 服务、报告读取调 health 服务、提醒注册调 reminder 服务（*Api 契约在各 facade）
@EnableFeignClients(basePackages = "com.xp.starsite.api.client")
@MapperScan("com.xp.starsite.recheck.mapper")
public class RecheckServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecheckServiceApplication.class, args);
    }
}
