package com.xp.starsite.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * 文件微服务启动类
 *
 * @author muyi
 * @since 2026-10
 */
@SpringBootApplication(scanBasePackages = "com.xp.starsite")
@EnableDiscoveryClient
// 上传完成事件（TOPIC_FILE）：显式装配，未配 rocketmq.name-server 时不生效
@Import(com.xp.starsite.common.mq.StarSiteMqConfig.class)
@MapperScan("com.xp.starsite.file.mapper")
public class FileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileServiceApplication.class, args);
    }
}
