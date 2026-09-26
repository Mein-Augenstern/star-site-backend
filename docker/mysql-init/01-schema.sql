-- MySQL 容器首次启动自动执行
CREATE DATABASE IF NOT EXISTS star_site DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE star_site;

CREATE TABLE IF NOT EXISTS `member` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`   VARCHAR(32)  NOT NULL COMMENT '登录名',
  `password`   VARCHAR(80)  NOT NULL COMMENT 'BCrypt 密文',
  `nickname`   VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '昵称',
  `avatar`     VARCHAR(255)          DEFAULT '' COMMENT '头像URL',
  `email`      VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
  `phone`      VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
  `gender`     TINYINT      NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
  `bio`        VARCHAR(255)          DEFAULT '' COMMENT '简介',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员表';
