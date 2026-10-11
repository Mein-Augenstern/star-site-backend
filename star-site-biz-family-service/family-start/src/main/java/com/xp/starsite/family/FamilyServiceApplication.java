package com.xp.starsite.family;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 家庭档案微服务启动类
 *
 * <p>scanBasePackages 扩到整个 starsite 包，复用 common 模块的 JwtUtil / 全局异常处理；
 * family-facade 的 FamilyOwnershipGuard 供其他业务服务引入做归属越权防线。
 *
 * @author xiaopu
 * @since 2026-10
 */
@SpringBootApplication(scanBasePackages = "com.xp.starsite")
@EnableDiscoveryClient
@MapperScan("com.xp.starsite.family.mapper")
public class FamilyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilyServiceApplication.class, args);
    }
}
