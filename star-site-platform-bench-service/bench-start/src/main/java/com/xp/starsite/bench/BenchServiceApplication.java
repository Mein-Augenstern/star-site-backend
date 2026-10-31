package com.xp.starsite.bench;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 组件压测微服务启动类：三组件的 RPC 层基准端点。
 * <p>
 * 部署后可通过 HTTP 压测工具（ab/wrk/python 脚本）对三组件做端到端性能验证；
 * 组件升级后重新部署本服务即可复用压测。
 *
 * @author muyi
 * @since 2026-10
 */
@SpringBootApplication(scanBasePackages = "com.xp.starsite")
@EnableDiscoveryClient
@EnableScheduling
public class BenchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BenchServiceApplication.class, args);
    }
}
