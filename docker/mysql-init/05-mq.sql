-- MQ 域：消费幂等（star-site-common AbstractIdempotentConsumer）
-- 注意：MySQL 容器 volume 已初始化过的话，本文件不会自动执行，需手动跑一次
USE star_site;

CREATE TABLE IF NOT EXISTS `event_consume` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `consumer_group` VARCHAR(64)  NOT NULL COMMENT '消费组',
  `event_id`       VARCHAR(64)  NOT NULL COMMENT '事件唯一id(信封)',
  `consumed_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消费时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_event` (`consumer_group`, `event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MQ 消费幂等表';
