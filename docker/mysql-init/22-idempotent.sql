-- starsite-idempotent 组件（五号组件）幂等表
-- 2026-11：MQ 消费幂等内核由 event_consume 手工表迁移至本组件（AbstractIdempotentConsumer）
-- 注意：MySQL 容器 volume 已初始化过的话，本文件不会自动执行；
-- 组件构造期 initDdl 也会 IF NOT EXISTS 自动建表（幂等可并存），本文件供迁移脚本管理制使用
USE star_site;

CREATE TABLE IF NOT EXISTS `idempotent_record` (
  `id`            BIGINT        NOT NULL COMMENT '主键（starsite-sequence 组件生成）',
  `scope`         VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '键作用域（隔离不同业务的同名键）',
  `idem_key`      VARCHAR(128)  NOT NULL COMMENT '幂等键（业务提供——orderId/请求 Idempotency-Key 头）',
  `status`        VARCHAR(16)   NOT NULL COMMENT 'PROCESSING/DONE/FAILED',
  `result`        TEXT          COMMENT '回放载荷（仅 DONE 有值——首次成功结果的序列化形态）',
  `error`         VARCHAR(500)  COMMENT '失败摘要（仅 FAILED 有值，类型+消息截断）',
  `expire_at`     DATETIME      NOT NULL COMMENT '键记忆窗口边界（认领时刻+key-ttl；过期即键遗忘）',
  `finished_at`   DATETIME      DEFAULT NULL COMMENT '终态时刻',
  `host`          VARCHAR(128)  NOT NULL COMMENT '认领实例 hostname:pid',
  `gmt_create`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间（PROCESSING 活跃信号——停滞判崩溃遗留）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idem_scope_key` (`scope`, `idem_key`) COMMENT '认领互斥：INSERT 冲突即他方持有',
  KEY `idx_idem_expire` (`expire_at`) COMMENT 'retention 清理：expire_at 过期圈定'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API 幂等键记录';
