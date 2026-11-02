package com.xp.starsite.reminder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 提醒中心微服务启动类。
 *
 * <p>2026-11-02 组件化迁移：每日扫描由 starsite-schedule 组件驱动
 * （@StarsiteScheduled 自带触发引擎，无需 @EnableScheduling——本服务已无
 * 原生 @Scheduled 任务）。
 *
 * @author muyi
 * @since 2026-10
 */
@SpringBootApplication(scanBasePackages = "com.xp.starsite")
@EnableDiscoveryClient
// 邮件触达取会员联系方式（MemberApi）
@EnableFeignClients(basePackages = "com.xp.starsite.api.client")
@MapperScan("com.xp.starsite.reminder.mapper")
public class ReminderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReminderServiceApplication.class, args);
    }
}
