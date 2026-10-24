package com.xp.starsite.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 支付微服务启动类（平台级收银台：统一支付单/退款/渠道回调状态机）
 *
 * @author muyi
 * @since 2026-10
 */
@SpringBootApplication(scanBasePackages = "com.xp.starsite")
@EnableDiscoveryClient
// 内部收银台契约被业务服务（保险代收/会员订阅）经 facade 消费；本服务当前不消费其他服务的 Feign，
// 开启扫描以对齐全服务装配惯例（facade 契约在 com.xp.starsite.api.client）
@EnableFeignClients(basePackages = "com.xp.starsite.api.client")
@MapperScan("com.xp.starsite.payment.mapper")
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
