-- starsite-async-command 组件命令表（表结构由组件仓统一管理，本文件与其保持一致）
-- 表规范：主键 id 由 starsite-sequence 组件生成（禁 AUTO_INCREMENT）；
--         标准时间列 gmt_create / gmt_modified；高频路径索引三件套。
USE star_site;

CREATE TABLE IF NOT EXISTS `async_command` (
  `id`           BIGINT UNSIGNED NOT NULL COMMENT '主键（starsite-sequence 组件生成）',
  `cmd_type`     VARCHAR(64)  NOT NULL COMMENT '命令类型=执行器路由键',
  `biz_key`      VARCHAR(128) DEFAULT '' COMMENT '业务键(幂等/追踪)',
  `payload`      TEXT         DEFAULT NULL COMMENT '命令载荷 JSON',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/DEAD',
  `max_retry`    INT          NOT NULL DEFAULT 5,
  `retry_count`  INT          NOT NULL DEFAULT 0,
  `next_run_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '到期时间(延迟队列锚点)',
  `last_error`   VARCHAR(500) DEFAULT NULL,
  `owner`        VARCHAR(64)  DEFAULT '' COMMENT '提交方服务名',
  `claim_token`  VARCHAR(64)  DEFAULT NULL COMMENT '调度认领令牌',
  `gmt_create`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status_next` (`status`, `next_run_at`) COMMENT '认领查询：按状态+到期扫',
  KEY `idx_type_biz` (`cmd_type`, `biz_key`) COMMENT '幂等检查：同类型同业务键计数',
  KEY `idx_claim_token` (`claim_token`) COMMENT '认领取回：按令牌取本批命令（非唯一——一批共用令牌）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='异步命令';
