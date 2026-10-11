package com.xp.starsite.reminder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 提醒中心微服务启动类
 *
 * @author xiaopu
 * @since 2026-10
 */
@SpringBootApplication(scanBasePackages = "com.xp.starsite")
@EnableDiscoveryClient
// 邮件触达取会员联系方式（MemberApi）
@EnableFeignClients(basePackages = "com.xp.starsite.api.client")
@EnableScheduling
@MapperScan("com.xp.starsite.reminder.mapper")
public class ReminderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReminderServiceApplication.class, args);
    }
}
