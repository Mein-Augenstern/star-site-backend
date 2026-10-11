-- star-async 异步命令组件（源自 asyncmd 的项目化重实现，库形态见 docs/方案-star-async异步命令组件.md）
USE star_site;

CREATE TABLE IF NOT EXISTS `async_command` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `cmd_type`    VARCHAR(64)  NOT NULL COMMENT '命令类型=执行器路由键',
  `biz_key`     VARCHAR(128) DEFAULT '' COMMENT '业务键(幂等/追踪)',
  `payload`     TEXT         DEFAULT NULL COMMENT '命令载荷 JSON',
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/DEAD',
  `max_retry`   INT          NOT NULL DEFAULT 5,
  `retry_count` INT          NOT NULL DEFAULT 0,
  `next_run_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '到期时间(延迟队列锚点)',
  `last_error`  VARCHAR(500) DEFAULT NULL,
  `owner`       VARCHAR(64)  DEFAULT '' COMMENT '提交方服务名',
  `claim_token` VARCHAR(64)  DEFAULT NULL COMMENT '调度认领令牌',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status_next` (`status`, `next_run_at`),
  KEY `idx_type_biz` (`cmd_type`, `biz_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='异步命令';
